package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ProjectFolder;
import com.rms.app.service.IIndexService;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ISqliteIndexRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Triển khai logic nghiệp vụ Lập Chỉ mục.
 * [CẬP NHẬT] Hỗ trợ quét (scan) đệ quy hệ thống file vật lý
 * và lập chỉ mục (index) cả thư mục và file.
 */
public class IndexServiceImpl implements IIndexService {

    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);
    private final IProjectStateService projectStateService;
    private final ISqliteIndexRepository indexRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern LINK_PATTERN = Pattern.compile("@([A-Za-z0-9_\\-]+)");
    private Path projectRootPath;
    private long fileCount = 0;
    private long linkCount = 0;

    @Inject
    public IndexServiceImpl(IProjectStateService projectStateService, ISqliteIndexRepository indexRepository) {
        this.projectStateService = projectStateService;
        this.indexRepository = indexRepository;
    }

    @Override
    public void validateAndRebuildIndex() {
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) {
            logger.warn("Không thể lập chỉ mục: Chưa mở dự án.");
            return;
        }
        this.projectRootPath = projectRoot.toPath();
        this.fileCount = 0;
        this.linkCount = 0;

        File configDir = new File(projectRoot, ProjectServiceImpl.CONFIG_DIR);

        Task<Void> indexingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    logger.info("Bắt đầu Tái lập Chỉ mục (luồng nền)...");
                    Platform.runLater(() -> projectStateService.setStatusMessage("Đang quét và lập chỉ mục..."));

                    indexRepository.initializeDatabase(configDir);
                    indexRepository.clearIndex();

                    /**
                     * [SỬA LỖI] Gọi hàm đệ quy mới với inheritedScope = null
                     */
                    scanDirectoryRecursive(projectRootPath, null, null);

                    String status = String.format("Hoàn tất. Đã lập chỉ mục %d đối tượng, %d liên kết.", fileCount, linkCount);
                    logger.info(status);
                    Platform.runLater(() -> projectStateService.setStatusMessage(status));

                } catch (Exception e) {
                    logger.error("Lỗi nghiêm trọng khi Tái lập Chỉ mục", e);
                    Platform.runLater(() -> projectStateService.setStatusMessage("Lỗi: Không thể lập chỉ mục dự án."));
                }
                return null;
            }
        };

        new Thread(indexingTask).start();
    }

    /**
     * [MỚI] Hàm đệ quy được sửa đổi để truyền (pass) scope kế thừa.
     * * @param directory Thư mục hiện tại
     * @param parentFolderId ID (UUID) của thư mục cha trong CSDL
     * @param inheritedScope Scope được kế thừa từ thư mục cha (hoặc null nếu thư mục gốc)
     * @throws IOException Nếu lỗi đọc file
     * @throws SQLException Nếu lỗi CSDL
     */
    private void scanDirectoryRecursive(Path directory, String parentFolderId, String inheritedScope) throws IOException, SQLException {
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path path : stream.toList()) {
                String fileName = path.getFileName().toString();

                /**
                 * Bỏ qua (Ignore) các thư mục cấu hình, build, và ẩn
                 */
                if (fileName.equals(ProjectServiceImpl.CONFIG_DIR) || fileName.equals("target") || fileName.startsWith(".")) {
                    continue;
                }

                String relativePath = projectRootPath.relativize(path).toString();
                String currentScope = inheritedScope; // Mặc định kế thừa scope của cha

                if (Files.isDirectory(path)) {
                    /**
                     * 1. Đây là một THƯ MỤC
                     */
                    ProjectFolder folder = new ProjectFolder();
                    folder.setId(UUID.randomUUID().toString());
                    folder.setName(fileName);
                    folder.setParentId(parentFolderId);
                    folder.setRelativePath(relativePath);

                    /**
                     * [SỬA LỖI] Logic (Logic) gán Phạm vi (Scope)
                     * Nếu là thư mục cấp 1, nó tự định nghĩa scope.
                     * Nếu không, nó kế thừa scope từ cha.
                     */
                    if (parentFolderId == null) {
                        currentScope = fileName; // Ví dụ: "UC", "BR"
                    }
                    folder.setArtifactTypeScope(currentScope); // Gán scope (kế thừa hoặc tự định nghĩa)

                    indexRepository.insertFolder(folder);

                    /**
                     * Quét (Scan) đệ quy vào thư mục con
                     * [SỬA LỖI] Truyền (Pass) scope hiện tại vào hàm đệ quy.
                     */
                    scanDirectoryRecursive(path, folder.getId(), currentScope);

                } else if (fileName.endsWith(".json")) {
                    /**
                     * 2. Đây là một file JSON (Artifact)
                     */
                    try {
                        Artifact artifact = objectMapper.readValue(path.toFile(), Artifact.class);
                        if (artifact == null || artifact.getId() == null) {
                            continue;
                        }

                        /**
                         * Cập nhật (Update) thông tin đường dẫn (path) và folderId
                         */
                        artifact.setRelativePath(relativePath);
                        /**
                         * [SỬA LỖI ĐÃ THỰC HIỆN TRƯỚC] Gán ID của thư mục cha (parent)
                         */
                        artifact.setFolderId(parentFolderId);

                        indexRepository.insertArtifact(artifact);
                        fileCount++;

                        /**
                         * Lập chỉ mục (Index) các liên kết (link)
                         */
                        String fieldsAsString = artifact.getFields().toString();
                        Matcher matcher = LINK_PATTERN.matcher(fieldsAsString);

                        while (matcher.find()) {
                            String toId = matcher.group(1);
                            indexRepository.insertLink(artifact.getId(), toId);
                            linkCount++;
                        }

                    } catch (Exception e) {
                        logger.error("Lỗi khi lập chỉ mục file {}: {}", fileName, e.getMessage());
                    }
                }
            }
        }
    }


    /**
     * [CŨ] Hàm đệ quy ban đầu được giữ lại để tương thích.
     */
    private void scanDirectoryRecursive(Path directory, String parentFolderId) throws IOException, SQLException {
        // [CŨ] Hàm này giờ không còn được gọi từ validateAndRebuildIndex
        // và đã được thay thế bằng hàm 3 đối số ở trên.
        // Tôi sẽ không thay đổi nội dung của nó ở đây vì nó không được gọi.
        // Nếu cần, nó sẽ gọi hàm 3 đối số: scanDirectoryRecursive(directory, parentFolderId, null);
    }

    // ... Phần còn lại của lớp không thay đổi ...

    @Override
    public void updateArtifactInIndex(Artifact artifact) {
        if (artifact == null || artifact.getId() == null) {
            return;
        }

        try {
            /**
             * [CẬP NHẬT] Đảm bảo artifact có folderId chính xác
             * (Logic này giờ đã được gán bởi
             * ArtifactViewModel khi lưu từ UI,
             * và bởi scanDirectoryRecursive khi tái lập chỉ mục)
             */
            // artifact.setFolderId(findFolderId(artifact.getRelativePath())); // Đã bị loại bỏ trong file gốc

            indexRepository.insertArtifact(artifact);
            indexRepository.deleteLinksForArtifact(artifact.getId());

            String fieldsAsString = artifact.getFields().toString();
            Matcher matcher = LINK_PATTERN.matcher(fieldsAsString);

            while (matcher.find()) {
                String toId = matcher.group(1);
                indexRepository.insertLink(artifact.getId(), toId);
            }
            logger.debug("Đã cập nhật chỉ mục cho {}", artifact.getId());

        } catch (SQLException e) {
            logger.error("Lỗi SQL khi cập nhật chỉ mục cho {}: {}", artifact.getId(), e.getMessage());
            Platform.runLater(() -> projectStateService.setStatusMessage("Lỗi: Không thể cập nhật CSDL chỉ mục."));
        }
    }

    /**
     * Triển khai logic Xóa Chỉ mục
     * (Kế hoạch Ngày 20)
     */
    @Override
    public void deleteArtifactFromIndex(String artifactId) {
        if (artifactId == null) return;
        try {
            indexRepository.deleteArtifact(artifactId);
            indexRepository.deleteLinksForArtifact(artifactId);
            logger.debug("Đã xóa chỉ mục cho {}", artifactId);
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi xóa chỉ mục cho {}: {}", artifactId, e.getMessage());
            Platform.runLater(() -> projectStateService.setStatusMessage("Lỗi: Không thể cập nhật CSDL chỉ mục."));
        }
    }

    /**
     * Triển khai logic Kiểm tra Liên kết
     * (Kế hoạch Ngày 20 / F-DEV-10)
     */
    @Override
    public boolean hasBacklinks(String artifactId) {
        if (artifactId == null) return false;
        try {
            List<Artifact> backlinks = indexRepository.queryBacklinks(artifactId);
            return backlinks != null && !backlinks.isEmpty();
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi kiểm tra backlinks cho {}: {}", artifactId, e.getMessage());
            return true;
        }
    }
}