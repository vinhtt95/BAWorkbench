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

public class TemplateServiceImpl implements ITemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final IProjectStateService projectStateService;

    @Inject
    public TemplateServiceImpl(IProjectStateService projectStateService) { // SỬA
        this.projectStateService = projectStateService; // SỬA
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
        // Tên file theo convention [TemplateName].template.json
        String safeName = templateName.toLowerCase().replaceAll("\\s+", "_");
        return safeName + ".template.json";
    }

    @Override
    public void saveTemplate(ArtifactTemplate template) throws IOException {
        File configDir = getConfigDirectory();
        File templateFile = new File(configDir, getTemplateFileName(template.getTemplateName()));

        logger.info("Đang lưu template: {}", templateFile.getPath());
        // 9.0. Chuyển đổi thiết kế Form GUI thành code cấu hình
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
}