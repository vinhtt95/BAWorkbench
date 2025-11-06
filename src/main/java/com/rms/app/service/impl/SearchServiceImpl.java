package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ISearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchServiceImpl implements ISearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    private final IProjectStateService projectStateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache (Index) trong bộ nhớ
    private final Map<String, Artifact> artifactCache = new HashMap<>();

    @Inject
    public SearchServiceImpl(IProjectStateService projectStateService) {
        this.projectStateService = projectStateService;

        // Lắng nghe khi dự án thay đổi để build lại index
        projectStateService.currentProjectDirectoryProperty().addListener((obs, oldDir, newDir) -> {
            if (newDir != null) {
                try {
                    buildIndex();
                } catch (IOException e) {
                    logger.error("Không thể build search index", e);
                }
            } else {
                artifactCache.clear();
            }
        });
    }

    @Override
    public void buildIndex() throws IOException {
        artifactCache.clear();
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) return;

        File artifactsDir = new File(projectRoot, ProjectServiceImpl.ARTIFACTS_DIR);
        if (!artifactsDir.exists() || !artifactsDir.isDirectory()) {
            logger.warn("Thư mục Artifacts/ không tồn tại, bỏ qua index.");
            return;
        }

        logger.info("Đang xây dựng Search Index...");
        try (Stream<Path> paths = Files.walk(artifactsDir.toPath())) {
            List<Path> jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());

            for (Path jsonFile : jsonFiles) {
                try {
                    Artifact artifact = objectMapper.readValue(jsonFile.toFile(), Artifact.class);
                    if (artifact != null && artifact.getId() != null) {
                        artifactCache.put(artifact.getId(), artifact);
                    }
                } catch (IOException e) {
                    logger.error("Không thể đọc artifact file: {}", jsonFile, e);
                }
            }
        }
        logger.info("Hoàn tất Index. Đã cache {} artifacts.", artifactCache.size());
    }

    @Override
    public List<Artifact> search(String query) {
        String normalizedQuery = query.toLowerCase().replace("@", "");
        return artifactCache.values().stream()
                .filter(artifact ->
                        artifact.getId().toLowerCase().contains(normalizedQuery) ||
                                artifact.getName().toLowerCase().contains(normalizedQuery)
                )
                .limit(10) // Giới hạn 10 kết quả
                .collect(Collectors.toList());
    }

    /**
     * [THÊM MỚI NGÀY 17] Triển khai logic getBacklinks (Phiên bản 1: Quét cache)
     */
    @Override
    public List<Artifact> getBacklinks(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return new ArrayList<>();
        }
        // Định dạng liên kết chuẩn, ví dụ: "@UC001"
        String fullLink = "@" + artifactId;
        List<Artifact> results = new ArrayList<>();

        for (Artifact artifact : artifactCache.values()) {
            // Bỏ qua, không tự liên kết đến chính mình
            if (artifact.getId().equals(artifactId)) {
                continue;
            }

            if (artifact.getFields() != null) {
                /**
                 * Đây là phiên bản 1 (theo kế hoạch ngày 17), quét chậm.
                 * Chúng ta chỉ cần kiểm tra xem chuỗi đại diện của map
                 * có chứa ID liên kết hay không.
                 * (Sẽ được tối ưu bằng SQLite ở Giai đoạn 5)
                 */
                String fieldsAsString = artifact.getFields().toString();
                if (fieldsAsString.contains(fullLink)) {
                    results.add(artifact);
                }
            }
        }
        return results;
    }
}