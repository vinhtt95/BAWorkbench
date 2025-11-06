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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Triển khai (implementation) logic nghiệp vụ Lập Chỉ mục (Kế hoạch Ngày 19).
 * Điều phối Repository File (.json) và Repository CSDL (.db).
 * [vinhtt95/baworkbench/BAWorkbench-b81f6c2eab10596eeb739d9111f5ef0610b2666e/Requirement/ImplementPlan.md]
 */
public class IndexServiceImpl implements IIndexService {

    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);
    private final IProjectStateService projectStateService;
    private final ISqliteIndexRepository indexRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Pattern Regex để tìm các link @ID (tham chiếu UC-DEV-02)
     * [vinhtt95/baworkbench/BAWorkbench-b81f6c2eab10596eeb739d9111f5ef0610b2666e/Requirement/UseCases/UC-DEV-03.md]
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

        Task<Void> indexingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    logger.info("Bắt đầu Tái lập Chỉ mục (luồng nền)...");
                    projectStateService.setStatusMessage("Đang quét và lập chỉ mục...");

                    indexRepository.initializeDatabase(configDir);
                    indexRepository.clearIndex();

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
                                Artifact artifact = objectMapper.readValue(jsonFile.toFile(), Artifact.class);
                                if (artifact == null || artifact.getId() == null) {
                                    continue;
                                }

                                indexRepository.insertArtifact(artifact);
                                fileCount++;

                                String fieldsAsString = artifact.getFields().toString();
                                Matcher matcher = LINK_PATTERN.matcher(fieldsAsString);

                                while (matcher.find()) {
                                    String toId = matcher.group(1);
                                    indexRepository.insertLink(artifact.getId(), toId);
                                    linkCount++;
                                }

                            } catch (Exception e) {
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

        new Thread(indexingTask).start();
    }

    /**
     * [THÊM MỚI NGÀY 20] Triển khai logic Triple-Write
     * (Kế hoạch Ngày 20)
     */
    @Override
    public void updateArtifactInIndex(Artifact artifact) {
        if (artifact == null || artifact.getId() == null) {
            return;
        }

        try {
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
            projectStateService.setStatusMessage("Lỗi: Không thể cập nhật CSDL chỉ mục.");
        }
    }

    /**
     * [THÊM MỚI NGÀY 20] Triển khai logic Xóa Chỉ mục
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
            projectStateService.setStatusMessage("Lỗi: Không thể cập nhật CSDL chỉ mục.");
        }
    }

    /**
     * [THÊM MỚI NGÀY 20] Triển khai logic Kiểm tra Liên kết
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