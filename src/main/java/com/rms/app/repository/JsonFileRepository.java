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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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

    // SỬA LỖI: Triển khai hàm helper
    private File getFileForArtifact(String id) throws IOException {
        File artifactsDir = getArtifactsRoot();
        return new File(artifactsDir, id + ".json");
    }

    // SỬA LỖI: Triển khai hàm save
    @Override
    public void save(Artifact artifact) throws IOException {
        if (artifact == null || artifact.getId() == null) {
            throw new IOException("Artifact hoặc Artifact ID không được null.");
        }

        File file = getFileForArtifact(artifact.getId());
        logger.debug("Đang lưu file: {}", file.getPath());

        // C-05: Dữ liệu PHẢI được lưu dưới dạng file .json (Source of Truth)
        objectMapper.writeValue(file, artifact);

        // TODO: Triển khai Dual-Write (Ngày 13)
        // C-06: Một file .md PHẢI được tự động sinh ra
        logger.warn("Hàm save() (phần .md) chưa được implement (Ngày 13).");
    }

    // SỬA LỖI: Triển khai hàm load
    @Override
    public Artifact load(String id) throws IOException {
        File file = getFileForArtifact(id);
        if (!file.exists()) {
            throw new IOException("File không tồn tại: " + id + ".json");
        }
        return objectMapper.readValue(file, Artifact.class);
    }

    // SỬA LỖI: Triển khai hàm delete
    @Override
    public void delete(String id) throws IOException {
        File jsonFile = getFileForArtifact(id);
        // TODO: Xóa cả file .md khi implement Dual-Write

        Files.deleteIfExists(jsonFile.toPath());
        logger.debug("Đã xóa: {}", jsonFile.getPath());
    }
}