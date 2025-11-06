package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rms.app.model.ProjectConfig;
import com.rms.app.service.IProjectService;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProjectServiceImpl implements IProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private final ObjectMapper objectMapper;

    public static final String CONFIG_DIR = ".config";
    public static final String ARTIFACTS_DIR = "Artifacts";
    public static final String CONFIG_FILE = "project.json";

    public ProjectServiceImpl() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public boolean createProject(String projectName, File directory) throws IOException {
        logger.info("Đang tạo dự án mới: {} tại {}", projectName, directory.getPath());

        // 5.0. Hệ thống tạo cấu trúc thư mục chuẩn
        File configDir = new File(directory, CONFIG_DIR);
        File artifactsDir = new File(directory, ARTIFACTS_DIR);

        if (!configDir.mkdirs() || !artifactsDir.mkdirs()) {
            throw new IOException("Không thể tạo thư mục dự án.");
        }

        // 6.0. Hệ thống khởi tạo các file cấu hình mặc định
        ProjectConfig config = new ProjectConfig();
        config.setProjectName(projectName);

        File configFile = new File(configDir, CONFIG_FILE);
        objectMapper.writeValue(configFile, config);

        // Tạo file .gitignore
        createGitIgnore(directory.toPath());

        return true;
    }

    @Override
    public ProjectConfig openProject(File directory) throws IOException {
        logger.info("Đang mở dự án tại: {}", directory.getPath());

        // 5.0. Hệ thống quét thư mục đã chọn, tìm file cấu hình
        File configFile = new File(directory, CONFIG_DIR + File.separator + CONFIG_FILE);

        if (!configFile.exists() || !configFile.isFile()) {
            // 5.1. Hệ thống không tìm thấy file cấu hình
            throw new IOException("Thư mục đã chọn không phải là một dự án RMS hợp lệ.");
        }

        // 6.0. Hệ thống tải (load) toàn bộ cấu hình
        return objectMapper.readValue(configFile, ProjectConfig.class);
    }

    @Override
    public TreeItem<String> buildProjectTree(File projectRoot) {
        // 5.0. Logic đọc cấu trúc thư mục và hiển thị lên TreeView
        TreeItem<String> root = new TreeItem<>(projectRoot.getName());
        root.setExpanded(true);

        File artifactsDir = new File(projectRoot, ARTIFACTS_DIR);
        if(artifactsDir.exists() && artifactsDir.isDirectory()) {
            TreeItem<String> artifactsNode = new TreeItem<>(ARTIFACTS_DIR);
            artifactsNode.setExpanded(true);
            root.getChildren().add(artifactsNode);
            // TODO: Quét sâu hơn để tìm các file .json
        }

        return root;
    }

    private void createGitIgnore(Path projectRoot) throws IOException {
        String gitIgnoreContent =
                "# Ignore IDE files\n" +
                        ".idea/\n" +
                        "*.iml\n" +
                        "\n" +
                        "# Ignore OS files\n" +
                        ".DS_Store\n" +
                        "\n" +
                        "# Ignore build artifacts\n" +
                        "target/\n" +
                        "*.log\n" +
                        "\n" +
                        "# Dữ liệu .json là Source of Truth (Git-Friendly Mirror là .md)\n" +
                        "# Chúng ta nên commit cả hai, nhưng nếu chỉ muốn commit .md, ta có thể ignore .json\n" +
                        "*.json\n";

        Files.writeString(projectRoot.resolve(".gitignore"), gitIgnoreContent);
    }
}