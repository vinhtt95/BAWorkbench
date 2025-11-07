package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rms.app.model.Artifact;
import com.rms.app.model.ProjectConfig;
import com.rms.app.model.ProjectFolder;
import com.rms.app.service.IIndexService;
import com.rms.app.service.IProjectService;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ISqliteIndexRepository; // [MỚI] Import
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException; // [MỚI] Import
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.google.inject.Inject;

/**
 * Triển khai logic nghiệp vụ Quản lý Dự án.
 * [CẬP NHẬT] Xây dựng TreeView từ CSDL Chỉ mục (Index DB)
 * hỗ trợ thư mục đa cấp.
 */
public class ProjectServiceImpl implements IProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final IIndexService indexService;
    private final IProjectStateService projectStateService;
    private final ISqliteIndexRepository indexRepository; // [MỚI]

    public static final String CONFIG_DIR = ".config";
    public static final String CONFIG_FILE = "project.json";

    private ProjectConfig currentProjectConfig;

    /**
     * [MỚI] Lớp (class) public lồng nhau (nested)
     * cho Thư mục (Folder)
     */
    public static class FolderTreeItem extends TreeItem<String> {
        private final String folderId; // UUID của thư mục
        private final String relativePath;
        private final String artifactTypeScope; // Ví dụ: "UC"

        public FolderTreeItem(String displayName, String folderId, String relativePath, String artifactTypeScope) {
            super(displayName);
            this.folderId = folderId;
            this.relativePath = relativePath;
            this.artifactTypeScope = artifactTypeScope;
        }

        public String getFolderId() {
            return folderId;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public String getArtifactTypeScope() {
            return artifactTypeScope;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }
    }


    /**
     * [CẬP NHẬT] Lớp (class) lồng nhau (nested) cho Artifact
     */
    public static class ArtifactTreeItem extends TreeItem<String> {
        private final String relativePath; // Ví dụ: "UC/Tài Khoản/UC001.json"

        public ArtifactTreeItem(String displayName, String relativePath) {
            super(displayName);
            this.relativePath = relativePath;
        }

        public String getRelativePath() {
            return relativePath;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }
    }


    @Inject
    public ProjectServiceImpl(IIndexService indexService,
                              IProjectStateService projectStateService,
                              ISqliteIndexRepository indexRepository) { // [MỚI] Inject
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.indexService = indexService;
        this.projectStateService = projectStateService;
        this.indexRepository = indexRepository; // [MỚI]
    }

    @Override
    public boolean createProject(String projectName, File directory) throws IOException {
        logger.info("Đang tạo dự án mới: {} tại {}", projectName, directory.getPath());

        File configDir = new File(directory, CONFIG_DIR);

        if (!configDir.mkdirs()) {
            throw new IOException("Không thể tạo thư mục .config.");
        }

        ProjectConfig config = new ProjectConfig();
        config.setProjectName(projectName);

        File configFile = new File(configDir, CONFIG_FILE);
        objectMapper.writeValue(configFile, config);

        createGitIgnore(directory.toPath());
        this.currentProjectConfig = config;
        return true;
    }

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

    /**
     * [TÁI CẤU TRÚC] Xây dựng TreeView từ CSDL Chỉ mục (Index DB)
     * bằng cách truy vấn đệ quy các thư mục và file.
     *
     * @param projectRoot Thư mục gốc
     * @return TreeItem (gốc)
     */
    @Override
    public TreeItem<String> buildProjectTree(File projectRoot) {
        TreeItem<String> root = new TreeItem<>(projectRoot.getName());
        root.setExpanded(true);

        try {
            /**
             * Bắt đầu quá trình đệ quy từ gốc (parentId = null)
             */
            buildTreeRecursively(root, null);
        } catch (SQLException e) {
            logger.error("Lỗi SQL nghiêm trọng khi xây dựng cây dự án. Cây có thể không đầy đủ.", e);
            root.getChildren().add(new TreeItem<>("LỖI KHI TẢI CÂY DỰ ÁN"));
        }

        return root;
    }

    /**
     * [MỚI] Hàm helper (hàm phụ) đệ quy để xây dựng cây.
     *
     * @param parentNode Nút (Node) TreeItem cha
     * @param parentFolderId ID (UUID) của thư mục cha trong CSDL
     * @throws SQLException Nếu lỗi CSDL
     */
    private void buildTreeRecursively(TreeItem<String> parentNode, String parentFolderId) throws SQLException {

        /**
         * 1. Lấy (Fetch) và thêm tất cả các THƯ MỤC CON (sub-folders)
         */
        List<ProjectFolder> folders = indexRepository.getFolders(parentFolderId);
        for (ProjectFolder folder : folders) {
            FolderTreeItem folderNode = new FolderTreeItem(
                    folder.getName(),
                    folder.getId(),
                    folder.getRelativePath(),
                    folder.getArtifactTypeScope()
            );
            folderNode.setExpanded(true); // Mở rộng (expand) thư mục
            parentNode.getChildren().add(folderNode);

            /**
             * Đệ quy (Recurse) vào thư mục con
             */
            buildTreeRecursively(folderNode, folder.getId());
        }

        /**
         * 2. Lấy (Fetch) và thêm tất cả các FILE (artifacts)
         */
        List<Artifact> artifacts = indexRepository.getArtifacts(parentFolderId);
        for (Artifact artifact : artifacts) {
            String displayName = (artifact.getName() != null && !artifact.getName().isEmpty())
                    ? artifact.getName()
                    : artifact.getId();

            ArtifactTreeItem artifactNode = new ArtifactTreeItem(
                    displayName,
                    artifact.getRelativePath()
            );
            parentNode.getChildren().add(artifactNode);
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