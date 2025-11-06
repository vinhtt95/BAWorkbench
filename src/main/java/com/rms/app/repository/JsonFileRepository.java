package com.rms.app.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.service.IArtifactRepository;
import com.rms.app.service.IIndexService;
import com.rms.app.service.IProjectStateService;
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

/**
 * Triển khai logic còn thiếu (TODOs) của Ngày 6
 */
public class JsonFileRepository implements IArtifactRepository {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileRepository.class);

    private final ObjectMapper objectMapper;
    private final IProjectStateService projectStateService;
    private final IIndexService indexService;

    @Inject
    public JsonFileRepository(IProjectStateService projectStateService, IIndexService indexService) {
        this.projectStateService = projectStateService;
        this.indexService = indexService;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Triển khai hàm helper
     */
    private File getArtifactsRoot() throws IOException {
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) {
            throw new IOException("Không có dự án nào đang mở.");
        }
        File artifactsDir = new File(projectRoot, ProjectServiceImpl.ARTIFACTS_DIR);
        if (!artifactsDir.exists()) {
            throw new IOException("Thư mục 'Artifacts' không tồn tại.");
        }
        return artifactsDir;
    }

    /**
     * Helper mới để lấy file bằng đường dẫn tương đối
     */
    private File getArtifactFile(String relativePath) throws IOException {
        File artifactsDir = getArtifactsRoot();
        return new File(artifactsDir, relativePath);
    }

    @Override
    public void save(Artifact artifact) throws IOException {
        if (artifact == null || artifact.getId() == null) {
            throw new IOException("Artifact hoặc Artifact ID không được null.");
        }

        String artifactType = artifact.getArtifactType();
        if (artifactType == null || artifactType.isEmpty()) {
            throw new IOException("ArtifactType không được rỗng để lưu vào thư mục con.");
        }

        File subDir = new File(getArtifactsRoot(), artifactType);
        subDir.mkdirs();

        Path jsonPath = new File(subDir, artifact.getId() + ".json").toPath();
        logger.debug("Đang lưu file: {}", jsonPath);

        objectMapper.writeValue(jsonPath.toFile(), artifact);

        Path mdPath = new File(subDir, artifact.getId() + ".md").toPath();
        String mdContent = generateMarkdown(artifact);
        Files.writeString(mdPath, mdContent);

        /**
         * [THÊM MỚI NGÀY 20] Hoàn thành Triple-Write (F-DEV-05)
         * [vinhtt95/baworkbench/BAWorkbench-b81f6c2eab10596eeb739d9111f5ef0610b2666e/Requirement/Functional Requirements Document.md]
         */
        indexService.updateArtifactInIndex(artifact);
    }

    /**
     * Helper tạo nội dung Markdown (đơn giản)
     */
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
    public Artifact load(String relativePath) throws IOException {
        File fileToLoad = getArtifactFile(relativePath);
        if (!fileToLoad.exists()) {
            throw new IOException("File không tồn tại: " + relativePath);
        }
        return objectMapper.readValue(fileToLoad, Artifact.class);
    }

    @Override
    public void delete(String relativePath) throws IOException {
        File jsonFile = getArtifactFile(relativePath);
        String id = jsonFile.getName().replace(".json", "");

        /**
         * [THÊM MỚI NGÀY 20] Kiểm tra Toàn vẹn (F-DEV-10, F-DEV-11)
         * [vinhtt95/baworkbench/BAWorkbench-b81f6c2eab10596eeb739d9111f5ef0610b2666e/Requirement/Functional Requirements Document.md]
         */
        if (indexService.hasBacklinks(id)) {
            logger.warn("Ngăn chặn xóa {}: Artifact đang có liên kết ngược.", id);
            throw new IOException("Không thể xóa " + id + ". Đối tượng đang được liên kết bởi các artifact khác.");
        }

        File mdFile = getArtifactFile(relativePath.replace(".json", ".md"));

        Files.deleteIfExists(jsonFile.toPath());
        Files.deleteIfExists(mdFile.toPath());
        logger.debug("Đã xóa file: {} (và file .md)", jsonFile.getPath());

        /**
         * [THÊM MỚI NGÀY 20] Xóa khỏi CSDL Chỉ mục
         */
        indexService.deleteArtifactFromIndex(id);
    }
}