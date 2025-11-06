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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemplateServiceImpl implements ITemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final IProjectStateService projectStateService;

    private static final String TEMPLATE_SUFFIX = ".template.json";

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
     * Quét tất cả file .template.json trong thư mục .config
     */
    private List<Path> getAllTemplateFiles() throws IOException {
        File configDir = getConfigDirectory();
        try (Stream<Path> paths = Files.walk(configDir.toPath(), 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(TEMPLATE_SUFFIX))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<String> loadAllTemplateNames() throws IOException {
        List<Path> templateFiles = getAllTemplateFiles();
        List<String> templateNames = new ArrayList<>();

        for (Path templateFile : templateFiles) {
            try {
                ArtifactTemplate template = objectMapper.readValue(templateFile.toFile(), ArtifactTemplate.class);
                if (template != null && template.getTemplateName() != null) {
                    templateNames.add(template.getTemplateName());
                }
            } catch (IOException e) {
                logger.error("Không thể đọc template file: {}", templateFile, e);
            }
        }
        return templateNames;
    }

    /**
     * [THÊM MỚI] Triển khai hàm loadTemplateByPrefix
     */
    @Override
    public ArtifactTemplate loadTemplateByPrefix(String prefix) throws IOException {
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }

        List<Path> templateFiles = getAllTemplateFiles();

        for (Path templateFile : templateFiles) {
            try {
                ArtifactTemplate template = objectMapper.readValue(templateFile.toFile(), ArtifactTemplate.class);
                if (template != null && prefix.equals(template.getPrefixId())) {
                    return template;
                }
            } catch (IOException e) {
                logger.error("Không thể đọc template file khi tìm prefix: {}", templateFile, e);
            }
        }

        logger.warn("Không tìm thấy template nào cho prefix: {}", prefix);
        return null;
    }
}