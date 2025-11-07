package com.rms.app.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.FlowStep;
import com.rms.app.service.IArtifactRepository;
import com.rms.app.service.IIndexService;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.impl.ProjectServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Triển khai logic I/O.
 * [CẬP NHẬT] Sửa đổi để lưu file dựa trên relativePath
 * thay vì tự động tạo thư mục con (sub-directory).
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
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Trả về thư mục gốc của dự án (Project Root)
     */
    private File getArtifactsRoot() throws IOException {
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) {
            throw new IOException("Không có dự án nào đang mở.");
        }
        if (!projectRoot.exists()) {
            throw new IOException("Thư mục dự án '" + projectRoot.getPath() + "' không tồn tại.");
        }
        return projectRoot;
    }

    /**
     * Helper lấy file bằng đường dẫn tương đối
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

        /**
         * [ĐÃ SỬA] Logic lưu file giờ đây dựa vào relativePath
         * được cung cấp bởi ViewModel.
         */
        if (artifact.getRelativePath() == null || artifact.getRelativePath().isEmpty()) {
            throw new IOException("Không thể lưu artifact: relativePath là null hoặc rỗng.");
        }

        File jsonFile = getArtifactFile(artifact.getRelativePath());

        /**
         * Đảm bảo các thư mục cha (parent directory) tồn tại
         */
        File parentDir = jsonFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        Path jsonPath = jsonFile.toPath();
        logger.debug("Đang lưu file: {}", jsonPath);

        objectMapper.writeValue(jsonPath.toFile(), artifact);

        /**
         * Lưu file .md tương ứng
         */
        Path mdPath = new File(parentDir, artifact.getId() + ".md").toPath();
        String mdContent = generateMarkdown(artifact);
        Files.writeString(mdPath, mdContent);

        /**
         * Hoàn thành Triple-Write (F-DEV-05)
         * (indexService sẽ đọc relativePath từ artifact)
         */
        indexService.updateArtifactInIndex(artifact);
    }

    /**
     * [KHÔNG THAY ĐỔI] Helper tạo nội dung Markdown.
     */
    private String generateMarkdown(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(artifact.getId()).append(": ").append(artifact.getName()).append("\n\n");

        for (Map.Entry<String, Object> entry : artifact.getFields().entrySet()) {
            sb.append("## ").append(entry.getKey()).append("\n");

            Object value = entry.getValue();

            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty()) {
                    try {
                        List<FlowStep> steps = objectMapper.convertValue(
                                list,
                                new TypeReference<List<FlowStep>>() {}
                        );

                        if (steps != null && !steps.isEmpty() && (steps.get(0).getActor() != null || steps.get(0).getAction() != null)) {
                            sb.append(formatFlowStepsToMarkdown(steps));
                        } else {
                            sb.append(value.toString());
                        }
                    } catch (Exception e) {
                        logger.warn("Không thể convert List sang FlowStep, in ra giá trị thô: {}", e.getMessage());
                        sb.append(value.toString());
                    }
                } else {
                    sb.append(value.toString());
                }
            } else {
                sb.append(value != null ? value.toString() : "*N/A*");
            }

            sb.append("\n\n");
        }
        return sb.toString();
    }

    /**
     * [KHÔNG THAY ĐỔI] Helper (hàm phụ) để định dạng (format)
     * một danh sách FlowStep thành bảng Markdown.
     *
     * @param steps Danh sách các bước (FlowStep)
     * @return Chuỗi (String) Markdown đã định dạng
     */
    private String formatFlowStepsToMarkdown(List<FlowStep> steps) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("| Actor / Logic | Action / Condition |\n");
            sb.append("|:---|:---|\n");

            for (FlowStep step : steps) {
                String actor = (step.getActor() != null) ? step.getActor() : "";
                String action = (step.getAction() != null) ? step.getAction() : "";

                if ("IF".equalsIgnoreCase(step.getLogicType()) || "ELSE".equalsIgnoreCase(step.getLogicType())) {
                    sb.append("| **").append(actor).append("** | **").append(action).append("** |\n");
                    if (step.getNestedSteps() != null) {
                        for (FlowStep nestedStep : step.getNestedSteps()) {
                            String nestedActor = (nestedStep.getActor() != null) ? nestedStep.getActor() : "";
                            String nestedAction = (nestedStep.getAction() != null) ? nestedStep.getAction() : "";
                            sb.append("| *&nbsp;&nbsp;&nbsp;&nbsp; ").append(nestedActor).append("* | *").append(nestedAction).append("* |\n");
                        }
                    }
                } else {
                    sb.append("| ").append(actor).append(" | ").append(action).append(" |\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            logger.warn("Lỗi khi định dạng (format) FlowSteps sang Markdown", e);
            try {
                return this.objectMapper.writeValueAsString(steps);
            } catch (Exception ex) {
                return "[Lỗi định dạng Flow]";
            }
        }
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
         * Kiểm tra Toàn vẹn (F-DEV-10, F-DEV-11)
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
         * Xóa khỏi CSDL Chỉ mục
         */
        indexService.deleteArtifactFromIndex(id);
    }

    @Override
    public String loadMarkdown(String relativePath) throws IOException {
        File fileToLoad = getArtifactFile(relativePath);
        if (!fileToLoad.exists()) {
            throw new IOException("File không tồn tại: " + relativePath);
        }
        return Files.readString(fileToLoad.toPath(), StandardCharsets.UTF_8);
    }
}