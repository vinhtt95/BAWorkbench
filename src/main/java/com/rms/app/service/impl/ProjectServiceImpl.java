package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rms.app.model.ProjectConfig;
import com.rms.app.service.IIndexService;
import com.rms.app.service.IProjectService;
import com.rms.app.service.IProjectStateService;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.inject.Inject;

/**
 * Triển khai (implementation) logic nghiệp vụ Quản lý Dự án (UC-PM-01, UC-PM-02).
 */
public class ProjectServiceImpl implements IProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final IIndexService indexService;
    private final IProjectStateService projectStateService;

    public static final String CONFIG_DIR = ".config";
    public static final String ARTIFACTS_DIR = "Artifacts";
    public static final String CONFIG_FILE = "project.json";

    /**
     * Lưu trữ config hiện tại trong bộ nhớ.
     */
    private ProjectConfig currentProjectConfig;

    @Inject
    public ProjectServiceImpl(IIndexService indexService, IProjectStateService projectStateService) {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.indexService = indexService;
        this.projectStateService = projectStateService;
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

        this.currentProjectConfig = config;

        return true;
    }

    /**
     * Helper lấy đường dẫn file cấu hình (project.json).
     *
     * @return File
     * @throws IOException Nếu chưa mở dự án
     */
    private File getConfigFile() throws IOException {
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) {
            throw new IOException("Không có dự án nào đang mở.");
        }
        return new File(projectRoot, CONFIG_DIR + File.separator + CONFIG_FILE);
    }


    @Override
    public ProjectConfig openProject(File directory) throws IOException {
        logger.info("Đang mở dự án tại: {}", directory.getPath());

        File configFile = new File(directory, CONFIG_DIR + File.separator + CONFIG_FILE);

        if (!configFile.exists() || !configFile.isFile()) {
            throw new IOException("Thư mục đã chọn không phải là một dự án RMS hợp lệ.");
        }

        ProjectConfig config = objectMapper.readValue(configFile, ProjectConfig.class);

        this.currentProjectConfig = config;

        return config;
    }

    @Override
    public void saveCurrentProjectConfig() throws IOException {
        if (this.currentProjectConfig == null) {
            throw new IOException("Không có cấu hình dự án nào đang tải để lưu.");
        }
        File configFile = getConfigFile();
        logger.info("Đang lưu cấu hình dự án (ví dụ: Releases) vào: {}", configFile.getPath());
        objectMapper.writeValue(configFile, this.currentProjectConfig);
    }

    /**
     * [THÊM MỚI] Triển khai (implementation)
     * logic lưu API Key (UC-CFG-04).
     *
     * @param apiKey API Key
     * @throws IOException Nếu lỗi lưu
     */
    @Override
    public void saveGeminiApiKey(String apiKey) throws IOException {
        if (this.currentProjectConfig == null) {
            throw new IOException("Vui lòng mở một dự án trước khi lưu API Key.");
        }
        this.currentProjectConfig.setGeminiApiKey(apiKey);
        saveCurrentProjectConfig();
    }


    @Override
    public ProjectConfig getCurrentProjectConfig() {
        return this.currentProjectConfig;
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

            addNodesRecursively(artifactsDir, artifactsNode);
        }

        return root;
    }

    /**
     * Hàm quét đệ quy để xây dựng TreeView
     *
     * @param dir    Thư mục hiện tại để quét
     * @param parent Nút (node) cha trong TreeView
     */
    private void addNodesRecursively(File dir, TreeItem<String> parent) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                TreeItem<String> dirNode = new TreeItem<>(file.getName());
                dirNode.setExpanded(true);
                parent.getChildren().add(dirNode);
                addNodesRecursively(file, dirNode);
            } else if (file.getName().endsWith(".json")) {
                TreeItem<String> fileNode = new TreeItem<>(file.getName());
                parent.getChildren().add(fileNode);
            }
        }
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