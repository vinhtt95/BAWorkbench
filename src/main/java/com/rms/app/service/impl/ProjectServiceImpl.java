package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rms.app.model.Artifact;
import com.rms.app.model.ProjectConfig;
import com.rms.app.service.IIndexService;
import com.rms.app.service.IProjectService;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ISearchService; // [MỚI] Import
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // [SỬA LỖI] ĐÃ THÊM IMPORT
import java.util.List;
import java.util.Map;
import com.google.inject.Inject;

/**
 * Triển khai (implementation) logic nghiệp vụ Quản lý Dự án (UC-PM-01, UC-PM-02).
 * [CẬP NHẬT] Bỏ thư mục Artifacts, xây dựng TreeView từ CSDL Chỉ mục.
 */
public class ProjectServiceImpl implements IProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final IIndexService indexService;
    private final IProjectStateService projectStateService;
    private final ISearchService searchService; // [MỚI]

    public static final String CONFIG_DIR = ".config";
    // public static final String ARTIFACTS_DIR = "Artifacts"; // [XÓA]
    public static final String CONFIG_FILE = "project.json";

    /**
     * Lưu trữ config hiện tại trong bộ nhớ.
     */
    private ProjectConfig currentProjectConfig;

    /**
     * [MỚI] Lớp (class) public lồng nhau (nested)
     * để MainView có thể truy cập (access)
     * và lấy (get) relativePath.
     */
    public static class ArtifactTreeItem extends TreeItem<String> {
        private final String relativePath; // Ví dụ: "UC/UC001.json"

        public ArtifactTreeItem(String displayName, String relativePath) {
            super(displayName);
            this.relativePath = relativePath;
        }

        public String getRelativePath() {
            return relativePath;
        }

        /**
         * Ghi đè (Override)
         * để đảm bảo nó luôn là một 'lá' (leaf)
         */
        @Override
        public boolean isLeaf() {
            return true;
        }
    }


    @Inject
    public ProjectServiceImpl(IIndexService indexService,
                              IProjectStateService projectStateService,
                              ISearchService searchService) { // [MỚI] Inject
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.indexService = indexService;
        this.projectStateService = projectStateService;
        this.searchService = searchService; // [MỚI]
    }

    @Override
    public boolean createProject(String projectName, File directory) throws IOException {
        logger.info("Đang tạo dự án mới: {} tại {}", projectName, directory.getPath());

        File configDir = new File(directory, CONFIG_DIR);
        // [XÓA] Bỏ thư mục Artifacts
        // File artifactsDir = new File(directory, ARTIFACTS_DIR);

        // [CẬP NHẬT] Chỉ tạo .config
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

    /**
     * [TÁI CẤU TRÚC] Xây dựng TreeView từ CSDL Chỉ mục (Index DB)
     * thay vì quét (scan) File System.
     *
     * @param projectRoot Thư mục gốc
     * @return TreeItem (gốc)
     */
    @Override
    public TreeItem<String> buildProjectTree(File projectRoot) {
        TreeItem<String> root = new TreeItem<>(projectRoot.getName());
        root.setExpanded(true);

        // [MỚI] Lấy (fetch) dữ liệu từ CSDL Chỉ mục (thông qua Service)
        Map<String, List<Artifact>> groupedData = searchService.getArtifactsGroupedByType();

        // Sắp xếp (sort) các thư mục (Type) theo Bảng chữ cái
        List<String> sortedTypes = new ArrayList<>(groupedData.keySet());
        Collections.sort(sortedTypes);

        for (String type : sortedTypes) {
            // [SỬA LỖI] Không setExpanded(true) cho thư mục rỗng
            List<Artifact> artifacts = groupedData.get(type);
            if (artifacts == null || artifacts.isEmpty()) {
                continue; // Bỏ qua nếu không có artifact nào
            }

            TreeItem<String> typeNode = new TreeItem<>(type);
            typeNode.setExpanded(true);
            root.getChildren().add(typeNode); // [MỚI] Thêm trực tiếp vào root


            // [MỚI] Sắp xếp (sort) các artifact (lá) theo Tên (Name)
            artifacts.sort(Comparator.comparing(
                    a -> (a.getName() != null ? a.getName() : a.getId()),
                    String.CASE_INSENSITIVE_ORDER
            ));

            for (Artifact artifact : artifacts) {
                // [MỚI] Hiển thị Tên (Name) (Goal 2)
                String displayName = (artifact.getName() != null && !artifact.getName().isEmpty())
                        ? artifact.getName()
                        : artifact.getId(); // Fallback (Dự phòng) về ID nếu Tên (Name) rỗng

                // [MỚI] Tạo đường dẫn (path) tương đối (ví dụ: "UC/UC001.json")
                String relativePath = type + File.separator + artifact.getId() + ".json";

                // [MỚI] Sử dụng ArtifactTreeItem tùy chỉnh
                TreeItem<String> artifactNode = new ArtifactTreeItem(displayName, relativePath);
                typeNode.getChildren().add(artifactNode);
            }
        }
        return root;
    }

    /**
     * [XÓA] Phương thức này không còn được sử dụng
     * vì TreeView hiện được xây dựng từ CSDL Chỉ mục (Index DB).
     */
    // private void addNodesRecursively(File dir, TreeItem<String> parent) { ... }


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