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
import java.util.stream.Stream;
import com.google.inject.Inject;
import com.rms.app.service.IIndexService;

public class ProjectServiceImpl implements IProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final IIndexService indexService;

    public static final String CONFIG_DIR = ".config";
    public static final String ARTIFACTS_DIR = "Artifacts";
    public static final String CONFIG_FILE = "project.json";

    @Inject
    public ProjectServiceImpl(IIndexService indexService) {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.indexService = indexService;
    }

    @Override
    public boolean createProject(String projectName, File directory) throws IOException {
        logger.info("Đang tạo dự án mới: {} tại {}", projectName, directory.getPath());

        File configDir = new File(directory, CONFIG_DIR);
        File artifactsDir = new File(directory, ARTIFACTS_DIR);

        if (!configDir.mkdirs() || !artifactsDir.mkdirs()) {
            throw new IOException("Không thể tạo thư mục dự án.");
        }

        ProjectConfig config = new ProjectConfig();
        config.setProjectName(projectName);

        File configFile = new File(configDir, CONFIG_FILE);
        objectMapper.writeValue(configFile, config);

        createGitIgnore(directory.toPath());

        return true;
    }

    @Override
    public ProjectConfig openProject(File directory) throws IOException {
        logger.info("Đang mở dự án tại: {}", directory.getPath());

        File configFile = new File(directory, CONFIG_DIR + File.separator + CONFIG_FILE);

        if (!configFile.exists() || !configFile.isFile()) {
            throw new IOException("Thư mục đã chọn không phải là một dự án RMS hợp lệ.");
        }

        ProjectConfig config = objectMapper.readValue(configFile, ProjectConfig.class);

        /**
         * Logic này sẽ chạy trên luồng nền (do IndexServiceImpl.java)
         */
        indexService.validateAndRebuildIndex();

        return config;
    }

    @Override
    public TreeItem<String> buildProjectTree(File projectRoot) {
        TreeItem<String> root = new TreeItem<>(projectRoot.getName());
        root.setExpanded(true);

        File artifactsDir = new File(projectRoot, ARTIFACTS_DIR);
        if(artifactsDir.exists() && artifactsDir.isDirectory()) {
            TreeItem<String> artifactsNode = new TreeItem<>(ARTIFACTS_DIR);
            artifactsNode.setExpanded(true);
            root.getChildren().add(artifactsNode);

            try (Stream<Path> paths = Files.walk(artifactsDir.toPath(), 1)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(jsonFile -> {
                            String fileName = jsonFile.getFileName().toString();
                            TreeItem<String> fileNode = new TreeItem<>(fileName);
                            artifactsNode.getChildren().add(fileNode);
                        });
            } catch (IOException e) {
                logger.error("Không thể quét thư mục Artifacts", e);
                artifactsNode.getChildren().add(new TreeItem<>("Lỗi khi tải..."));
            }
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
                        "# [THÊM MỚI NGÀY 19] Bỏ qua file CSDL Chỉ mục (C-11)\n" +
                        "/.config/index.db\n" +
                        "\n" +
                        "# Dữ liệu .json là Source of Truth (Git-Friendly Mirror là .md)\n" +
                        "# Chúng ta nên commit cả hai, nhưng nếu chỉ muốn commit .md, ta có thể ignore .json\n" +
                        "# *.json\n";

        Files.writeString(projectRoot.resolve(".gitignore"), gitIgnoreContent);
    }
}