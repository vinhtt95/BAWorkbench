package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ITemplateService;
import com.rms.app.viewmodel.MainViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files; // Thêm import
import java.nio.file.Path; // Thêm import
import java.util.ArrayList; // Thêm import
import java.util.List; // Thêm import
import java.util.stream.Collectors; // Thêm import
import java.util.stream.Stream; // Thêm import

public class TemplateServiceImpl implements ITemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final IProjectStateService projectStateService;

    private static final String TEMPLATE_SUFFIX = ".template.json"; // Thêm hằng số

    @Inject
    public TemplateServiceImpl(IProjectStateService projectStateService) {
        this.projectStateService = projectStateService;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Lấy thư mục .config của dự án đang mở
     */
    private File getConfigDirectory() throws IOException {
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) {
            throw new IOException("Không có dự án nào đang mở.");
        }
        File configDir = new File(projectRoot, ProjectServiceImpl.CONFIG_DIR);
        if (!configDir.exists()) {
            throw new IOException("Thư mục .config không tồn tại.");
        }
        return configDir;
    }

    private String getTemplateFileName(String templateName) {
        String safeName = templateName.toLowerCase().replaceAll("\\s+", "_");
        return safeName + TEMPLATE_SUFFIX;
    }

    @Override
    public void saveTemplate(ArtifactTemplate template) throws IOException {
        File configDir = getConfigDirectory();
        File templateFile = new File(configDir, getTemplateFileName(template.getTemplateName()));

        logger.info("Đang lưu template: {}", templateFile.getPath());
        objectMapper.writeValue(templateFile, template);
    }

    @Override
    public ArtifactTemplate loadTemplate(String templateName) throws IOException {
        File configDir = getConfigDirectory();
        File templateFile = new File(configDir, getTemplateFileName(templateName));

        if (!templateFile.exists()) {
            throw new IOException("Template file không tồn tại: " + templateFile.getPath());
        }

        return objectMapper.readValue(templateFile, ArtifactTemplate.class);
    }

    /**
     * [THÊM MỚI] Triển khai hàm loadAllTemplateNames
     */
    @Override
    public List<String> loadAllTemplateNames() throws IOException {
        File configDir = getConfigDirectory();
        List<String> templateNames = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(configDir.toPath(), 1)) {
            List<Path> templateFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(TEMPLATE_SUFFIX))
                    .collect(Collectors.toList());

            for (Path templateFile : templateFiles) {
                try {
                    /**
                     * Đọc file template chỉ để lấy tên (templateName)
                     */
                    ArtifactTemplate template = objectMapper.readValue(templateFile.toFile(), ArtifactTemplate.class);
                    if (template != null && template.getTemplateName() != null) {
                        templateNames.add(template.getTemplateName());
                    }
                } catch (IOException e) {
                    logger.error("Không thể đọc template file: {}", templateFile, e);
                }
            }
        }
        return templateNames;
    }
}