package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.ExportTemplate;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ITemplateService;
import com.rms.app.viewmodel.MainViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Triển khai (implementation) logic nghiệp vụ
 * quản lý cả Form Template (UC-CFG-01) và Export Template (UC-CFG-03).
 */
public class TemplateServiceImpl implements ITemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final IProjectStateService projectStateService;

    private static final String FORM_TEMPLATE_SUFFIX = ".form.template.json";
    private static final String EXPORT_TEMPLATE_SUFFIX = ".export.template.json";

    @Inject
    public TemplateServiceImpl(IProjectStateService projectStateService) {
        this.projectStateService = projectStateService;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Lấy thư mục .config của dự án đang mở
     *
     * @return Thư mục .config
     * @throws IOException Nếu chưa mở dự án
     */
    private File getConfigDirectory() throws IOException {
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) {
            throw new IOException("Không có dự án nào đang mở.");
        }
        File configDir = new File(projectRoot, ProjectServiceImpl.CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        return configDir;
    }

    /**
     * Helper (hàm phụ) chuẩn hóa tên file
     *
     * @param templateName Tên template
     * @return Tên file an toàn
     */
    private String sanitizeTemplateName(String templateName) {
        return templateName.toLowerCase().replaceAll("\\s+", "_");
    }

    /**
     * Quét tất cả file template dựa trên Hậu tố (Suffix)
     *
     * @param suffix (ví dụ: ".form.template.json")
     * @return Danh sách các Path (Đường dẫn)
     * @throws IOException Nếu lỗi I/O
     */
    private List<Path> getAllTemplateFilesBySuffix(String suffix) throws IOException {
        File configDir = getConfigDirectory();
        try (Stream<Path> paths = Files.walk(configDir.toPath(), 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(suffix))
                    .collect(Collectors.toList());
        }
    }

    // --- Form Template (UC-CFG-01) ---

    @Override
    public void saveTemplate(ArtifactTemplate template) throws IOException {
        File configDir = getConfigDirectory();
        String fileName = sanitizeTemplateName(template.getTemplateName()) + FORM_TEMPLATE_SUFFIX;
        File templateFile = new File(configDir, fileName);

        logger.info("Đang lưu Form template: {}", templateFile.getPath());
        objectMapper.writeValue(templateFile, template);
    }

    @Override
    public ArtifactTemplate loadTemplate(String templateName) throws IOException {
        File configDir = getConfigDirectory();
        String fileName = sanitizeTemplateName(templateName) + FORM_TEMPLATE_SUFFIX;
        File templateFile = new File(configDir, fileName);

        if (!templateFile.exists()) {
            throw new IOException("Template file không tồn tại: " + templateFile.getPath());
        }

        return objectMapper.readValue(templateFile, ArtifactTemplate.class);
    }

    @Override
    public List<String> loadAllTemplateNames() throws IOException {
        List<Path> templateFiles = getAllTemplateFilesBySuffix(FORM_TEMPLATE_SUFFIX);
        List<String> templateNames = new ArrayList<>();

        for (Path templateFile : templateFiles) {
            try {
                ArtifactTemplate template = objectMapper.readValue(templateFile.toFile(), ArtifactTemplate.class);
                if (template != null && template.getTemplateName() != null) {
                    templateNames.add(template.getTemplateName());
                }
            } catch (IOException e) {
                logger.error("Không thể đọc form template file: {}", templateFile, e);
            }
        }
        return templateNames;
    }

    @Override
    public ArtifactTemplate loadTemplateByPrefix(String prefix) throws IOException {
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        List<Path> templateFiles = getAllTemplateFilesBySuffix(FORM_TEMPLATE_SUFFIX);

        for (Path templateFile : templateFiles) {
            try {
                ArtifactTemplate template = objectMapper.readValue(templateFile.toFile(), ArtifactTemplate.class);
                if (template != null && prefix.equals(template.getPrefixId())) {
                    return template;
                }
            } catch (IOException e) {
                logger.error("Không thể đọc form template file khi tìm prefix: {}", templateFile, e);
            }
        }
        logger.warn("Không tìm thấy template nào cho prefix: {}", prefix);
        return null;
    }

    // --- Export Template (UC-CFG-03) [THÊM MỚI NGÀY 32] ---

    @Override
    public void saveExportTemplate(ExportTemplate template) throws IOException {
        File configDir = getConfigDirectory();
        String fileName = sanitizeTemplateName(template.getTemplateName()) + EXPORT_TEMPLATE_SUFFIX;
        File templateFile = new File(configDir, fileName);

        logger.info("Đang lưu Export template: {}", templateFile.getPath());
        objectMapper.writeValue(templateFile, template);
    }

    @Override
    public ExportTemplate loadExportTemplate(String templateName) throws IOException {
        File configDir = getConfigDirectory();
        String fileName = sanitizeTemplateName(templateName) + EXPORT_TEMPLATE_SUFFIX;
        File templateFile = new File(configDir, fileName);

        if (!templateFile.exists()) {
            throw new IOException("Export Template file không tồn tại: " + templateFile.getPath());
        }

        return objectMapper.readValue(templateFile, ExportTemplate.class);
    }

    @Override
    public List<String> loadAllExportTemplateNames() throws IOException {
        List<Path> templateFiles = getAllTemplateFilesBySuffix(EXPORT_TEMPLATE_SUFFIX);
        List<String> templateNames = new ArrayList<>();

        for (Path templateFile : templateFiles) {
            try {
                ExportTemplate template = objectMapper.readValue(templateFile.toFile(), ExportTemplate.class);
                if (template != null && template.getTemplateName() != null) {
                    templateNames.add(template.getTemplateName());
                }
            } catch (IOException e) {
                logger.error("Không thể đọc export template file: {}", templateFile, e);
            }
        }
        return templateNames;
    }
}