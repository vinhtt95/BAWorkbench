package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.ProjectConfig;
import com.rms.app.model.ProjectFolder; // [MỚI] Import model thư mục
import com.rms.app.service.*;
import com.rms.app.service.impl.ProjectServiceImpl; // [MỚI] Import các lớp TreeItem
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID; // [MỚI] Import UUID

/**
 * "Brain" - Logic UI cho MainView.
 * Quản lý trạng thái chung của ứng dụng.
 * [CẬP NHẬT] Tái cấu trúc (refactor) để hỗ trợ
 * tạo/xóa/điều hướng cây thư mục đa cấp.
 */
public class MainViewModel {

    private static final Logger logger = LoggerFactory.getLogger(MainViewModel.class);
    private final IProjectService projectService;
    private final ITemplateService templateService;
    private final IViewManager viewManager;
    private final IProjectStateService projectStateService;
    private final IArtifactRepository artifactRepository;
    private final ISearchService searchService;
    private final IIndexService indexService;
    private final IExportService exportService;
    private final ISqliteIndexRepository sqliteIndexRepository; // [MỚI]

    private final ObjectProperty<TreeItem<String>> projectRoot;
    private final ObjectProperty<ProjectConfig> currentProject;
    private final ObjectProperty<File> currentProjectDirectory;

    private final ObservableList<Artifact> currentBacklinks = FXCollections.observableArrayList();
    private TabPane mainTabPane;

    @Inject
    public MainViewModel(IProjectService projectService,
                         ITemplateService templateService,
                         IViewManager viewManager,
                         IProjectStateService projectStateService,
                         IArtifactRepository artifactRepository,
                         ISearchService searchService,
                         IIndexService indexService,
                         IExportService exportService,
                         ISqliteIndexRepository sqliteIndexRepository) { // [MỚI]
        this.projectService = projectService;
        this.templateService = templateService;
        this.viewManager = viewManager;
        this.projectStateService = projectStateService;
        this.artifactRepository = artifactRepository;
        this.searchService = searchService;
        this.indexService = indexService;
        this.exportService = exportService;
        this.sqliteIndexRepository = sqliteIndexRepository; // [MỚI]

        this.projectRoot = new SimpleObjectProperty<>(new TreeItem<>("Chưa mở dự án"));
        this.currentProject = new SimpleObjectProperty<>(null);
        this.currentProjectDirectory = new SimpleObjectProperty<>(null);

        /**
         * Lắng nghe (listener) sự kiện trạng thái (status)
         * để tự động làm mới (refresh) cây.
         */
        projectStateService.statusMessageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && (
                    newMsg.startsWith("Đã lưu") ||
                            newMsg.startsWith("Import hoàn tất") ||
                            newMsg.startsWith("Hoàn tất.") ||
                            newMsg.startsWith("Đã xóa") ||
                            newMsg.startsWith("Đã tạo thư mục")
            )) {
                refreshProjectTree();
            }
        });
    }

    /**
     * Hàm refresh cây thư mục (TreeView).
     */
    private void refreshProjectTree() {
        File projectDir = projectStateService.getCurrentProjectDirectory();
        if (projectDir != null) {
            try {
                TreeItem<String> rootNode = projectService.buildProjectTree(projectDir);
                projectRoot.set(rootNode);
                logger.debug("Project tree refreshed.");
            } catch (Exception e) {
                logger.error("Lỗi tự động refresh cây thư mục", e);
            }
        }
    }

    /**
     * Getter cho Backlinks
     *
     * @return Danh sách (Observable) các backlinks (Artifacts)
     */
    public ObservableList<Artifact> getCurrentBacklinks() {
        return currentBacklinks;
    }

    /**
     * Logic cập nhật Backlinks khi đổi Tab
     *
     * @param selectedTab Tab hiện tại đang được chọn
     */
    public void updateBacklinks(Tab selectedTab) {
        currentBacklinks.clear();
        if (selectedTab == null) {
            return;
        }

        Object userData = selectedTab.getUserData();
        String artifactId = null;
        if (userData instanceof String) {
            artifactId = (String) userData;
        }

        if (artifactId == null) {
            String tabText = selectedTab.getText();
            if (tabText == null || "Welcome".equals(tabText) || tabText.startsWith("New ") || tabText.startsWith("Form Builder") || tabText.startsWith("Releases Config") || tabText.startsWith("Dashboard") || tabText.startsWith("Export Templates") || tabText.startsWith("Import Wizard") || tabText.startsWith("Project Graph")) {
                return;
            }
            artifactId = tabText;
        }

        try {
            List<Artifact> backlinks = searchService.getBacklinks(artifactId);
            if (!backlinks.isEmpty()) {
                currentBacklinks.addAll(backlinks);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi tải backlinks cho {}: {}", artifactId, e.getMessage());
        }
    }

    /**
     * Logic nghiệp vụ cho UC-PM-01
     *
     * @param projectName Tên dự án
     * @param directory Thư mục
     */
    public void createNewProject(String projectName, File directory) {
        try {
            boolean success = projectService.createProject(projectName, directory);
            if (success) {
                openProject(directory);
                projectStateService.setStatusMessage("Tạo dự án mới thành công: " + projectName);
            }
        } catch (IOException e) {
            logger.error("Lỗi tạo dự án", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * [MỚI] Logic tạo một Thư mục con (Sub-folder) mới
     *
     * @param selectedItem Mục (Item) TreeView (Thư mục)
     * @param folderName Tên thư mục mới
     */
    public void createNewFolder(TreeItem<String> selectedItem, String folderName) {
        try {
            File projectRoot = projectStateService.getCurrentProjectDirectory();
            if (projectRoot == null) {
                throw new IOException("Chưa mở dự án.");
            }

            /**
             * 1. Xác định thư mục cha (parent)
             */
            String parentFolderId = null;
            String parentRelativePath = "";
            String artifactTypeScope = null;

            if (selectedItem instanceof ProjectServiceImpl.FolderTreeItem) {
                ProjectServiceImpl.FolderTreeItem parentFolder = (ProjectServiceImpl.FolderTreeItem) selectedItem;
                parentFolderId = parentFolder.getFolderId();
                parentRelativePath = parentFolder.getRelativePath();
                artifactTypeScope = parentFolder.getArtifactTypeScope(); // Kế thừa (Inherit) scope
            } else if (selectedItem.getParent() == null) {
                // Đây là gốc (root) - parentId vẫn là null, path là ""
            } else {
                showErrorAlert("Lỗi Tạo Thư mục", "Không thể tạo thư mục bên trong một artifact. Vui lòng chọn một thư mục.");
                return;
            }

            /**
             * 2. Tạo đường dẫn (path) vật lý
             */
            String newRelativePath = Path.of(parentRelativePath, folderName).toString();
            File newFolderFile = new File(projectRoot, newRelativePath);

            if (newFolderFile.exists()) {
                throw new IOException("Thư mục '" + folderName + "' đã tồn tại.");
            }

            /**
             * 3. Tạo thư mục vật lý
             */
            if (!newFolderFile.mkdirs()) {
                throw new IOException("Không thể tạo thư mục trên đĩa: " + newRelativePath);
            }

            /**
             * 4. Tạo đối tượng (object) Model
             */
            ProjectFolder folder = new ProjectFolder();
            folder.setId(UUID.randomUUID().toString());
            folder.setName(folderName);
            folder.setParentId(parentFolderId);
            folder.setRelativePath(newRelativePath);
            folder.setArtifactTypeScope(artifactTypeScope); // Thư mục con kế thừa (inherit) scope (ví dụ: UC)

            /**
             * 5. Lưu vào CSDL Chỉ mục (Index DB)
             */
            sqliteIndexRepository.insertFolder(folder);

            /**
             * 6. Cập nhật UI (thêm vào cây)
             */
            ProjectServiceImpl.FolderTreeItem folderNode = new ProjectServiceImpl.FolderTreeItem(
                    folder.getName(), folder.getId(), folder.getRelativePath(), folder.getArtifactTypeScope()
            );
            selectedItem.getChildren().add(folderNode);
            selectedItem.setExpanded(true);
            projectStateService.setStatusMessage("Đã tạo thư mục: " + folderName);

        } catch (Exception e) {
            logger.error("Lỗi khi tạo thư mục mới", e);
            showErrorAlert("Lỗi Tạo Thư mục", e.getMessage());
        }
    }


    /**
     * [CẬP NHẬT] Logic tạo Artifact MỚI
     *
     * @param templateName Tên template (ví dụ: "Use Case")
     * @param selectedItem Mục (Item) TreeView (Thư mục)
     */
    public void createNewArtifact(String templateName, TreeItem<String> selectedItem) {
        try {
            if (selectedItem == null) {
                showErrorAlert("Lỗi Tạo Artifact", "Vui lòng chọn một thư mục (ví dụ: UC, BR) để tạo artifact.");
                return;
            }

            ArtifactTemplate template = templateService.loadLatestTemplateByName(templateName);
            if (template == null) {
                throw new IOException("Không tìm thấy template (phiên bản mới nhất) cho: " + templateName);
            }

            /**
             * [MỚI] Xác định thư mục đích (destination folder) và kiểm tra scope
             */
            ProjectServiceImpl.FolderTreeItem targetFolder = null;
            String targetFolderPath = "";
            String artifactTypeScope = null;

            if (selectedItem instanceof ProjectServiceImpl.ArtifactTreeItem) {
                /**
                 * Nếu click vào file, lấy thư mục cha (parent)
                 */
                targetFolder = (ProjectServiceImpl.FolderTreeItem) selectedItem.getParent();
            } else if (selectedItem instanceof ProjectServiceImpl.FolderTreeItem) {
                /**
                 * Nếu click vào thư mục
                 */
                targetFolder = (ProjectServiceImpl.FolderTreeItem) selectedItem;
            } else if (selectedItem.getParent() == null) {
                /**
                 * Click vào gốc (root)
                 */
                showErrorAlert("Lỗi Tạo Artifact", "Không thể tạo artifact ở gốc (root). Vui lòng chọn một thư mục (ví dụ: UC, BR).");
                return;
            }

            if (targetFolder != null) {
                targetFolderPath = targetFolder.getRelativePath();
                artifactTypeScope = targetFolder.getArtifactTypeScope();
            }

            /**
             * [MỚI] Kiểm tra Scope
             * (Chỉ các thư mục gốc (root folder) mới có scope)
             */
            if (artifactTypeScope != null && !artifactTypeScope.equals(template.getPrefixId())) {
                throw new IOException("Không thể tạo '" + template.getTemplateName() + "' (" + template.getPrefixId() +
                        ") bên trong thư mục '" + targetFolder.getValue() + "'. " +
                        "Thư mục này chỉ chấp nhận loại: " + artifactTypeScope);
            }

            /**
             * Mở tab (Tab) (sử dụng implementation cũ)
             */
            Tab newTab = viewManager.openArtifactTab(template);

            /**
             * [MỚI] Truyền thông tin đường dẫn (path) và scope vào tab
             * để ArtifactViewModel (File 11) có thể lấy
             */
            newTab.setUserData(Map.of(
                    "isNew", true,
                    "template", template,
                    "parentRelativePath", targetFolderPath,
                    "parentFolderId", (targetFolder != null ? targetFolder.getFolderId() : null)
            ));

            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Không thể tạo artifact: " + templateName, e);
            showErrorAlert("Lỗi Tạo Artifact", e.getMessage());
        }
    }

    /**
     * Logic mở Artifact đã tồn tại
     *
     * @param relativePath Đường dẫn tương đối (ví dụ: "UC/Tài Khoản/UC001.json")
     */
    public void openArtifact(String relativePath) {
        if (relativePath == null || !relativePath.endsWith(".json")) {
            logger.warn("Bỏ qua mở file không hợp lệ: {}", relativePath);
            return;
        }
        String id = new File(relativePath).getName().replace(".json", "");

        /**
         * Kiểm tra tab đã mở (dựa trên ID)
         */
        for (Tab tab : mainTabPane.getTabs()) {
            /**
             * [SỬA LỖI] Phải kiểm tra kiểu (type) của UserData
             * vì "New Artifact" giờ đây cũng dùng UserData (nhưng là Map)
             */
            if (tab.getUserData() instanceof String && id.equals(tab.getUserData())) {
                mainTabPane.getSelectionModel().select(tab);
                logger.info("Tab cho {} đã mở, chuyển sang tab này.", id);
                return;
            }
        }

        try {
            Artifact artifact = artifactRepository.load(relativePath);
            if (artifact == null) {
                throw new IOException("Không tìm thấy artifact: " + relativePath);
            }

            String templateId = artifact.getTemplateId();
            ArtifactTemplate template;
            if (templateId == null || templateId.isEmpty()) {
                logger.warn("Artifact {} không có templateId. Đang thử tải (load) " +
                        "phiên bản mới nhất theo prefix...", artifact.getId());
                template = templateService.loadLatestTemplateByPrefix(artifact.getArtifactType());
            } else {
                template = templateService.loadTemplateById(templateId);
            }

            if (template == null) {
                throw new IOException("Không tìm thấy template (ID: " + (templateId != null ? templateId : "null") + ") " +
                        "cho loại: " + artifact.getArtifactType());
            }
            Tab newTab = viewManager.openArtifactTab(artifact, template);
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Lỗi mở artifact: " + relativePath, e);
            showErrorAlert("Lỗi Mở File", e.getMessage());
        }
    }

    /**
     * [CẬP NHẬT] Mở một artifact bằng ID của nó.
     *
     * @param artifactId ID của artifact (ví dụ: "UC001")
     */
    public void openArtifactById(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return;
        }
        try {
            /**
             * [ĐÃ SỬA] Dùng SearchService để tìm artifact
             */
            List<Artifact> results = searchService.search(artifactId);
            Artifact foundArtifact = null;

            /**
             * Tìm (Find) khớp (match) ID chính xác
             */
            for(Artifact a : results) {
                if(a.getId().equalsIgnoreCase(artifactId)) {
                    foundArtifact = a;
                    break;
                }
            }

            if (foundArtifact == null || foundArtifact.getRelativePath() == null) {
                throw new IOException("Không tìm thấy artifact (hoặc relativePath) cho ID: " + artifactId);
            }

            openArtifact(foundArtifact.getRelativePath());

        } catch (Exception e) {
            logger.error("Không thể drill-down đến {}: {}", artifactId, e.getMessage());
            showErrorAlert("Lỗi Điều hướng", e.getMessage());
        }
    }


    /**
     * [MỚI] Logic nghiệp vụ Xóa Thư mục hoặc Artifact
     *
     * @param selectedItem Mục (Item) TreeView cần xóa
     * @throws IOException Nếu xóa thất bại
     */
    public void deleteTreeItem(TreeItem<String> selectedItem) throws IOException {
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) {
            throw new IOException("Chưa mở dự án.");
        }

        if (selectedItem instanceof ProjectServiceImpl.ArtifactTreeItem) {
            /**
             * 1. XÓA FILE (ARTIFACT)
             */
            ProjectServiceImpl.ArtifactTreeItem item = (ProjectServiceImpl.ArtifactTreeItem) selectedItem;
            String relativePath = item.getRelativePath();

            /**
             * Gọi Repository (Repository tự kiểm tra backlink,
             * xóa file .json, .md, và xóa CSDL)
             */
            artifactRepository.delete(relativePath);

            /**
             * Đóng tab (tab) đang mở (nếu có)
             */
            String id = new File(relativePath).getName().replace(".json", "");
            closeTabById(id);

            projectStateService.setStatusMessage("Đã xóa: " + id); // Kích hoạt refresh

        } else if (selectedItem instanceof ProjectServiceImpl.FolderTreeItem) {
            /**
             * 2. XÓA THƯ MỤC (FOLDER)
             */
            ProjectServiceImpl.FolderTreeItem item = (ProjectServiceImpl.FolderTreeItem) selectedItem;
            String folderId = item.getFolderId();
            String relativePath = item.getRelativePath();
            Path folderPath = projectRoot.toPath().resolve(relativePath);

            /**
             * [TODO] Cần logic kiểm tra backlink
             * cho TẤT CẢ artifact con bên trong
             * (Hiện tại bỏ qua để đơn giản hóa)
             */

            /**
             * 2a. Xóa khỏi CSDL Chỉ mục (Index DB)
             */
            try {
                sqliteIndexRepository.deleteFolder(folderId);
            } catch (SQLException e) {
                throw new IOException("Lỗi CSDL khi xóa thư mục: " + e.getMessage());
            }

            /**
             * 2b. Xóa thư mục vật lý (physical folder)
             */
            if (Files.exists(folderPath)) {
                Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        /**
                         * Đóng (Close) các tab (tab) con đang mở
                         */
                        if(file.toString().endsWith(".json")) {
                            String id = file.getFileName().toString().replace(".json", "");
                            closeTabById(id);
                        }
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            projectStateService.setStatusMessage("Đã xóa thư mục: " + item.getValue()); // Kích hoạt refresh
        }
    }

    /**
     * [MỚI] Helper (hàm phụ) đóng (close) một tab (nếu đang mở) bằng ID.
     */
    private void closeTabById(String artifactId) {
        Tab tabToClose = null;
        for (Tab tab : mainTabPane.getTabs()) {
            if (artifactId.equals(tab.getUserData())) {
                tabToClose = tab;
                break;
            }
        }
        if (tabToClose != null) {
            mainTabPane.getTabs().remove(tabToClose);
        }
    }


    /**
     * Logic nghiệp vụ cho UC-PM-02
     *
     * @param directory Thư mục dự án
     */
    public void openProject(File directory) {
        try {
            if (mainTabPane != null) {
                mainTabPane.getTabs().clear();
                Tab welcomeTab = new Tab("Welcome");
                welcomeTab.setContent(new Label("Chào mừng đến với RMS v1.0"));
                mainTabPane.getTabs().add(welcomeTab);
                mainTabPane.getSelectionModel().select(welcomeTab);
            }
            ProjectConfig config = projectService.openProject(directory);
            currentProject.set(config);
            projectStateService.setCurrentProjectDirectory(directory);
            indexService.validateAndRebuildIndex(); // Kích hoạt quét (scan) đệ quy
            /**
             * refreshProjectTree() sẽ được gọi tự động
             * bởi listener khi indexService.validateAndRebuildIndex()
             * hoàn tất và setStatusMessage.
             */
        } catch (IOException e) {
            logger.error("Lỗi mở dự án", e);
            showErrorAlert("Lỗi Mở Dự án", e.getMessage());
        }
    }

    /**
     * Mở tab Cấu hình Form Builder (UC-CFG-01).
     */
    public void openFormBuilderTab() {
        try {
            Tab newTab = viewManager.openViewInNewTab(
                    "/com/rms/app/view/FormBuilderView.fxml", "Form Builder"
            );
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Không thể tải FormBuilderView", e);
        }
    }

    /**
     * Mở tab Cấu hình Releases (UC-CFG-02).
     */
    public void openReleasesConfigTab() {
        try {
            Tab newTab = viewManager.openViewInNewTab(
                    "/com/rms/app/view/ReleasesView.fxml", "Releases Config"
            );
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Không thể tải ReleasesView", e);
        }
    }

    /**
     * Mở tab Bảng Kanban (UC-MGT-02).
     */
    public void openDashboardTab() {
        try {
            Tab newTab = viewManager.openViewInNewTab(
                    "/com/rms/app/view/DashboardView.fxml", "Dashboard"
            );
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Không thể tải DashboardView", e);
        }
    }

    /**
     * Mở tab Sơ đồ Quan hệ (UC-MOD-02).
     */
    public void openGraphViewTab() {
        if (projectStateService.getCurrentProjectDirectory() == null) {
            showErrorAlert("Lỗi", "Vui lòng mở một dự án trước.");
            return;
        }
        try {
            Tab newTab = viewManager.openViewInNewTab(
                    "/com/rms/app/view/ProjectGraphView.fxml", "Project Graph"
            );
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Không thể tải ProjectGraphView", e);
        }
    }

    /**
     * Logic nghiệp vụ cho UC-PUB-02 (Xuất Excel).
     */
    public void exportProjectToExcel() {
        if (mainTabPane == null || mainTabPane.getScene() == null || mainTabPane.getScene().getWindow() == null) {
            showErrorAlert("Lỗi", "Không thể mở hộp thoại lưu file.");
            return;
        }
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Project to Excel");
        fileChooser.setInitialFileName(projectStateService.getCurrentProjectDirectory().getName() + "_Export.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                List<String> allTemplateNames = templateService.loadAllTemplateNames();
                if (allTemplateNames.isEmpty()) {
                    showErrorAlert("Lỗi Xuất", "Không có loại (template) nào để xuất.");
                    return;
                }
                Task<Void> exportTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        exportService.exportToExcel(file, allTemplateNames);
                        return null;
                    }
                };
                new Thread(exportTask).start();
            } catch (IOException e) {
                logger.error("Không thể tải danh sách template để xuất.", e);
                showErrorAlert("Lỗi Xuất", e.getMessage());
            }
        }
    }

    /**
     * Mở tab Trình thiết kế Template Xuất bản (UC-CFG-03).
     */
    public void openExportTemplateBuilderTab() {
        try {
            Tab newTab = viewManager.openViewInNewTab(
                    "/com/rms/app/view/ExportTemplateBuilderView.fxml", "Export Templates"
            );
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Không thể tải ExportTemplateBuilderView", e);
        }
    }

    /**
     * Mở Dialog "Tùy chọn Xuất bản" (UC-PUB-01, Bước 2.0).
     */
    public void openExportToDocumentDialog() {
        if (projectStateService.getCurrentProjectDirectory() == null) {
            showErrorAlert("Lỗi", "Vui lòng mở một dự án trước.");
            return;
        }

        try {
            /**
             * Bước 3.0 & 4.0: Tải dữ liệu cho Dropdown
             */
            List<String> templateNames = templateService.loadAllExportTemplateNames();
            List<Map<String, String>> releases = projectService.getCurrentProjectConfig().getReleases();

            ObservableList<String> releaseOptions = FXCollections.observableArrayList();
            releaseOptions.add("Không lọc (Tất cả yêu cầu)"); // 1.0.A1
            if (releases != null) {
                releases.forEach(rel -> releaseOptions.add(rel.get("id") + ": " + rel.get("name")));
            }

            if (templateNames.isEmpty()) {
                showErrorAlert("Lỗi", "Không tìm thấy 'Template Xuất bản' (UC-CFG-03).");
                return;
            }

            /**
             * Xây dựng Dialog (Cửa sổ)
             */
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Tùy chọn Xuất bản (UC-PUB-01)");
            dialog.setHeaderText("Chọn template và bộ lọc để xuất tài liệu.");
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            ComboBox<String> templateCombo = new ComboBox<>(FXCollections.observableArrayList(templateNames));
            templateCombo.setPromptText("Chọn Template Xuất bản");
            templateCombo.setMinWidth(300);
            if (!templateNames.isEmpty()) {
                templateCombo.getSelectionModel().selectFirst();
            }

            ComboBox<String> releaseCombo = new ComboBox<>(releaseOptions);
            releaseCombo.setPromptText("Chọn Lọc theo Release");
            releaseCombo.setMinWidth(300);
            releaseCombo.getSelectionModel().selectFirst();

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.add(new Label("Template:"), 0, 0);
            grid.add(templateCombo, 1, 0);
            grid.add(new Label("Lọc Release:"), 0, 1);
            grid.add(releaseCombo, 1, 1);

            dialogPane.setContent(grid);

            /**
             * Xử lý kết quả (Bước 6.0)
             */
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                String selectedTemplate = templateCombo.getValue();
                String selectedReleaseOption = releaseCombo.getValue();

                if (selectedTemplate == null || selectedTemplate.isEmpty()) {
                    showErrorAlert("Lỗi", "Bạn phải chọn một template.");
                    return;
                }

                String releaseIdFilter = null;
                if (selectedReleaseOption != null && !selectedReleaseOption.startsWith("Không lọc")) {
                    releaseIdFilter = selectedReleaseOption.split(":")[0].trim();
                }

                /**
                 * Bước 5.0: Chọn vị trí lưu file
                 */
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Lưu Tài liệu");
                fileChooser.setInitialFileName("My_SRS_Export.pdf");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"),
                        new FileChooser.ExtensionFilter("Word Files (*.docx)", "*.docx")
                );
                File file = fileChooser.showSaveDialog(mainTabPane.getScene().getWindow());

                if (file != null) {
                    /**
                     * Bước 7.0: Kích hoạt (trên luồng nền)
                     */
                    final String finalReleaseIdFilter = releaseIdFilter;
                    Task<Void> exportTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            exportService.exportToDocument(file, selectedTemplate, finalReleaseIdFilter);
                            return null;
                        }

                        @Override
                        protected void failed() {
                            showErrorAlert("Lỗi Xuất bản", getException().getMessage());
                            logger.error("Lỗi Xuất bản (UC-PUB-01)", getException());
                        }
                    };
                    new Thread(exportTask).start();
                }
            }

        } catch (Exception e) {
            showErrorAlert("Lỗi", e.getMessage());
            logger.error("Không thể mở Dialog Xuất bản", e);
        }
    }

    /**
     * Mở tab Trình hướng dẫn (Wizard) Import (UC-PM-03).
     */
    public void openImportWizardTab() {
        if (projectStateService.getCurrentProjectDirectory() == null) {
            showErrorAlert("Lỗi", "Vui lòng mở một dự án trước khi Import.");
            return;
        }

        try {
            Tab newTab = viewManager.openViewInNewTab(
                    "/com/rms/app/view/ImportWizardView.fxml", "Import Wizard"
            );
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Không thể tải ImportWizardView", e);
        }
    }

    /**
     * Mở Dialog (Cửa sổ) Cấu hình API Key (UC-CFG-04).
     */
    public void openApiKeysDialog() {
        if (projectStateService.getCurrentProjectDirectory() == null) {
            showErrorAlert("Lỗi", "Vui lòng mở một dự án trước.");
            return;
        }

        ProjectConfig config = projectService.getCurrentProjectConfig();
        String currentKey = config.getGeminiApiKey() != null ? config.getGeminiApiKey() : "";

        TextInputDialog dialog = new TextInputDialog(currentKey);
        dialog.setTitle("Cấu hình API Keys");
        dialog.setHeaderText("Quản lý API Key (UC-CFG-04)");
        dialog.setContentText("Google Gemini API Key:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(apiKey -> {
            try {
                projectService.saveGeminiApiKey(apiKey);
                projectStateService.setStatusMessage("Đã lưu Gemini API Key.");
            } catch (IOException e) {
                logger.error("Không thể lưu API Key", e);
                showErrorAlert("Lỗi Lưu", e.getMessage());
            }
        });
    }

    /**
     * Kích hoạt (Trigger) UC-PM-04 (Tái lập Chỉ mục).
     */
    public void rebuildIndex() {
        if (projectStateService.getCurrentProjectDirectory() == null) {
            showErrorAlert("Lỗi", "Vui lòng mở một dự án trước khi Tái lập Chỉ mục.");
            return;
        }
        /**
         * Service này đã tự xử lý (handle) luồng nền (background thread)
         */
        indexService.validateAndRebuildIndex();
    }


    public ObjectProperty<TreeItem<String>> projectRootProperty() {
        return projectRoot;
    }

    public void setMainTabPane(TabPane mainTabPane) {
        this.mainTabPane = mainTabPane;
    }

    /**
     * Hàm helper hiển thị Alert Lỗi cho người dùng.
     *
     * @param title   Tiêu đề của Alert
     * @param content Nội dung lỗi
     */
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}