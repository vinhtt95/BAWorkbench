package com.rms.app.repository;

import com.fasterxml.jackson.core.type.TypeReference; // [THÊM MỚI] Import
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
         * Hoàn thành Triple-Write (F-DEV-05)
         */
        indexService.updateArtifactInIndex(artifact);
    }

    /**
     * [SỬA LỖI LẦN 2] Helper tạo nội dung Markdown.
     * Hiện đã hỗ trợ định dạng (format) FlowStep,
     * ngay cả khi 'value' là List<Map> (được tải từ file).
     */
    private String generateMarkdown(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(artifact.getId()).append(": ").append(artifact.getName()).append("\n\n");

        for (Map.Entry<String, Object> entry : artifact.getFields().entrySet()) {
            sb.append("## ").append(entry.getKey()).append("\n");

            Object value = entry.getValue();

            /**
             * [SỬA LỖI LẦN 2] Kiểm tra xem đây có phải là một Flow (danh sách) không
             */
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty()) {
                    /**
                     * Cố gắng convert (chuyển đổi) danh sách (List) này thành List<FlowStep>.
                     * Điều này xử lý trường hợp 'value' là List<Map> (khi tải từ file)
                     * VÀ trường hợp 'value' là List<FlowStep> (khi lưu từ ViewModel).
                     */
                    try {
                        List<FlowStep> steps = objectMapper.convertValue(
                                list,
                                new TypeReference<List<FlowStep>>() {}
                        );

                        /**
                         * Nếu convert thành công VÀ có vẻ là FlowStep (có actor hoặc action)
                         * (Kiểm tra 'get(0)' là an toàn vì chúng ta đã kiểm tra 'isEmpty()')
                         */
                        if (steps != null && (steps.get(0).getActor() != null || steps.get(0).getAction() != null)) {
                            sb.append(formatFlowStepsToMarkdown(steps));
                        } else {
                            /**
                             * Không phải là FlowStep, in ra như cũ
                             */
                            sb.append(value.toString());
                        }
                    } catch (Exception e) {
                        /**
                         * Không thể convert, đây là một danh sách (List) thông thường.
                         */
                        logger.warn("Không thể convert List sang FlowStep, in ra giá trị thô: {}", e.getMessage());
                        sb.append(value.toString());
                    }
                } else {
                    /**
                     * Danh sách rỗng, in ra như cũ
                     */
                    sb.append(value.toString());
                }
            } else {
                /**
                 * Không phải là danh sách (List), giữ nguyên hành vi cũ
                 */
                sb.append(value != null ? value.toString() : "*N/A*");
            }

            sb.append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Helper (hàm phụ) để định dạng (format)
     * một danh sách FlowStep thành bảng Markdown.
     * (Tuân thủ CodingConvention về Javadoc)
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

                /**
                 * Xử lý logic IF/ELSE (làm cho chúng nổi bật)
                 */
                if ("IF".equalsIgnoreCase(step.getLogicType()) || "ELSE".equalsIgnoreCase(step.getLogicType())) {
                    sb.append("| **").append(actor).append("** | **").append(action).append("** |\n");

                    /**
                     * Thêm các bước lồng nhau (nested) với thụt đầu dòng (indent)
                     */
                    if (step.getNestedSteps() != null) {
                        for (FlowStep nestedStep : step.getNestedSteps()) {
                            String nestedActor = (nestedStep.getActor() != null) ? nestedStep.getActor() : "";
                            String nestedAction = (nestedStep.getAction() != null) ? nestedStep.getAction() : "";
                            /**
                             * Dùng &nbsp; để lùi vào
                             */
                            sb.append("| *&nbsp;&nbsp;&nbsp;&nbsp; ").append(nestedActor).append("* | *").append(nestedAction).append("* |\n");
                        }
                    }
                } else {
                    /**
                     * Bước (step) thông thường
                     */
                    sb.append("| ").append(actor).append(" | ").append(action).append(" |\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            logger.warn("Lỗi khi định dạng (format) FlowSteps sang Markdown", e);
            /**
             * Fallback (Phương án dự phòng): Trả về JSON (vẫn tốt hơn là object reference)
             */
            try {
                /**
                 * Sử dụng objectMapper đã tồn tại trong lớp
                 */
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