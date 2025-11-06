package com.rms.app.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.service.IArtifactRepository;
import com.rms.app.service.impl.ProjectServiceImpl;
import com.rms.app.viewmodel.MainViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rms.app.model.Artifact;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

// SỬA LỖI: Triển khai logic còn thiếu (TODOs) của Ngày 6
public class JsonFileRepository implements IArtifactRepository {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileRepository.class);

    private final ObjectMapper objectMapper;
    private final MainViewModel mainViewModel; // Để biết dự án hiện tại đang mở

    @Inject
    public JsonFileRepository(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    // SỬA LỖI: Triển khai hàm helper
    private File getArtifactsRoot() throws IOException {
        File projectRoot = mainViewModel.getCurrentProjectDirectory();
        if (projectRoot == null) {
            throw new IOException("Không có dự án nào đang mở.");
        }
        File artifactsDir = new File(projectRoot, ProjectServiceImpl.ARTIFACTS_DIR);
        if (!artifactsDir.exists()) {
            throw new IOException("Thư mục 'Artifacts' không tồn tại.");
        }
        return artifactsDir;
    }

    private File getFileForArtifact(String id) throws IOException {
        File artifactsDir = getArtifactsRoot();
        return new File(artifactsDir, id + ".json");
    }

    private Path getArtifactPath(String id, String extension) throws IOException {
        File artifactsDir = getArtifactsRoot();
        return new File(artifactsDir, id + extension).toPath();
    }

    @Override
    public void save(Artifact artifact) throws IOException {
        if (artifact == null || artifact.getId() == null) {
            throw new IOException("Artifact hoặc Artifact ID không được null.");
        }

        Path jsonPath = getArtifactPath(artifact.getId(), ".json");
        logger.debug("Đang lưu file: {}", jsonPath);

        // C-05: Dữ liệu PHẢI được lưu dưới dạng file .json (Source of Truth)
        objectMapper.writeValue(jsonPath.toFile(), artifact);

        // NGÀY 13: Logic Dual-Write (Git-Friendly)
        // C-06: Một file .md PHẢI được tự động sinh ra

        Path mdPath = getArtifactPath(artifact.getId(), ".md");
        String mdContent = generateMarkdown(artifact);
        Files.writeString(mdPath, mdContent);
    }

    // Helper tạo nội dung Markdown (đơn giản)
    private String generateMarkdown(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(artifact.getId()).append(": ").append(artifact.getName()).append("\n\n");

        for (Map.Entry<String, Object> entry : artifact.getFields().entrySet()) {
            sb.append("## ").append(entry.getKey()).append("\n");
            sb.append(entry.getValue() != null ? entry.getValue().toString() : "*N/A*");
            sb.append("\n\n");
        }
        return sb.toString();
    }


    @Override
    public Artifact load(String id) throws IOException {
        Path jsonPath = getArtifactPath(id, ".json");
        if (!Files.exists(jsonPath)) {
            throw new IOException("File không tồn tại: " + id + ".json");
        }
        return objectMapper.readValue(jsonPath.toFile(), Artifact.class);
    }

    @Override
    public void delete(String id) throws IOException {
        Path jsonPath = getArtifactPath(id, ".json");
        Path mdPath = getArtifactPath(id, ".md");

        Files.deleteIfExists(jsonPath);
        Files.deleteIfExists(mdPath);
        logger.debug("Đã xóa: {} (và file .md)", jsonPath);
    }
}