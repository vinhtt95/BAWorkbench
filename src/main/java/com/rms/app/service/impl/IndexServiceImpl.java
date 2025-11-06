package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.service.IIndexService;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ISqliteIndexRepository;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Triển khai (implementation) logic nghiệp vụ Lập Chỉ mục (Kế hoạch Ngày 19).
 * Điều phối Repository File (.json) và Repository CSDL (.db).
 * [vinhtt95/baworkbench/BAWorkbench-8718cda1cf4b17075f34bedd73e1e9e86e29ecc3/Requirement/ImplementPlan.md]
 */
public class IndexServiceImpl implements IIndexService {

    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);
    private final IProjectStateService projectStateService;
    private final ISqliteIndexRepository indexRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Pattern Regex để tìm các link @ID (tham chiếu UC-DEV-02)
     * [vinhtt95/baworkbench/BAWorkbench-8718cda1cf4b17075f34bedd73e1e9e86e29ecc3/Requirement/UseCases/UC-DEV-02.md]
     */
    private static final Pattern LINK_PATTERN = Pattern.compile("@([A-Za-z0-9_\\-]+)");

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

        File configDir = new File(projectRoot, ProjectServiceImpl.CONFIG_DIR);
        File artifactsDir = new File(projectRoot, ProjectServiceImpl.ARTIFACTS_DIR);

        /**
         * Triển khai logic quét trên Luồng nền (Task)
         * Tuân thủ NFR (Concurrency) [vinhtt95/baworkbench/BAWorkbench-8718cda1cf4b17075f34bedd73e1e9e86e29ecc3/Requirement/CodingConvention.md]
         */
        Task<Void> indexingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    logger.info("Bắt đầu Tái lập Chỉ mục (luồng nền)...");
                    projectStateService.setStatusMessage("Đang quét và lập chỉ mục...");

                    // 1. Khởi tạo CSDL (Ngày 18)
                    indexRepository.initializeDatabase(configDir);

                    // 2. Xóa sạch CSDL (Ngày 18)
                    indexRepository.clearIndex();

                    // 3. Quét thư mục
                    if (!artifactsDir.exists()) {
                        logger.warn("Thư mục 'Artifacts' không tồn tại. Bỏ qua quét.");
                        return null;
                    }

                    long fileCount = 0;
                    long linkCount = 0;

                    try (Stream<Path> paths = Files.walk(artifactsDir.toPath())) {
                        var jsonFiles = paths
                                .filter(Files::isRegularFile)
                                .filter(path -> path.toString().endsWith(".json"))
                                .toList();

                        for (Path jsonFile : jsonFiles) {
                            try {
                                // 4. Đọc file .json (Source of Truth - NFR-01)
                                // [vinhtt95/baworkbench/BAWorkbench-8718cda1cf4b17075f34bedd73e1e9e86e29ecc3/Requirement/Functional Requirements Document.md]
                                Artifact artifact = objectMapper.readValue(jsonFile.toFile(), Artifact.class);
                                if (artifact == null || artifact.getId() == null) {
                                    continue;
                                }

                                // 5. Ghi vào bảng 'artifacts' (Ngày 18)
                                indexRepository.insertArtifact(artifact);
                                fileCount++;

                                // 6. Phân tích (parse) links từ Map Fields
                                String fieldsAsString = artifact.getFields().toString();
                                Matcher matcher = LINK_PATTERN.matcher(fieldsAsString);

                                while (matcher.find()) {
                                    String toId = matcher.group(1);
                                    // 7. Ghi vào bảng 'links' (Ngày 18)
                                    indexRepository.insertLink(artifact.getId(), toId);
                                    linkCount++;
                                }

                            } catch (Exception e) {
                                // (Tham chiếu UC-PM-04 1.0.E1: Bỏ qua file lỗi, tiếp tục)
                                // [vinhtt95/baworkbench/BAWorkbench-8718cda1cf4b17075f34bedd73e1e9e86e29ecc3/Requirement/UseCases/UC-PM-04.md]
                                logger.error("Lỗi khi lập chỉ mục file {}: {}", jsonFile.getFileName(), e.getMessage());
                            }
                        }
                    }

                    String status = String.format("Hoàn tất. Đã lập chỉ mục %d đối tượng, %d liên kết.", fileCount, linkCount);
                    logger.info(status);
                    projectStateService.setStatusMessage(status);

                } catch (Exception e) {
                    logger.error("Lỗi nghiêm trọng khi Tái lập Chỉ mục", e);
                    projectStateService.setStatusMessage("Lỗi: Không thể lập chỉ mục dự án.");
                }
                return null;
            }
        };

        // Kích hoạt luồng nền
        new Thread(indexingTask).start();
    }

    /**
     * Các hàm cho Ngày 20
     */

    @Override
    public void updateArtifactInIndex(Artifact artifact) {
        // (Sẽ triển khai ở Ngày 20)
        logger.warn("updateArtifactInIndex() chưa được triển khai.");
    }

    @Override
    public void deleteArtifactFromIndex(String artifactId) {
        // (Sẽ triển khai ở Ngày 20)
        logger.warn("deleteArtifactFromIndex() chưa được triển khai.");
    }

    @Override
    public boolean hasBacklinks(String artifactId) {
        // (Sẽ triển khai ở Ngày 20)
        logger.warn("hasBacklinks() chưa được triển khai.");
        return false;
    }
}