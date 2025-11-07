package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.ProjectConfig;
import com.rms.app.service.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * "Brain" - Logic UI cho MainView.
 * Quản lý trạng thái chung của ứng dụng.
 */
public class MainViewModel {

    private static final Logger logger = LoggerFactory.getLogger(MainViewModel.class);
    private final IProjectService projectService;

    private final ObjectProperty<TreeItem<String>> projectRoot;
    private final StringProperty statusMessage;
    private final ObjectProperty<ProjectConfig> currentProject;

    private final ObjectProperty<File> currentProjectDirectory;
    private final ITemplateService templateService;
    private final IViewManager viewManager;
    private final IProjectStateService projectStateService;
    private final IArtifactRepository artifactRepository;
    private final ISearchService searchService;
    private final IIndexService indexService;
    private final IExportService exportService;

    private final ObservableList<String> currentBacklinks = FXCollections.observableArrayList();

    private TabPane mainTabPane;

    @Inject
    public MainViewModel(IProjectService projectService,
                         ITemplateService templateService,
                         IViewManager viewManager,
                         IProjectStateService projectStateService,
                         IArtifactRepository artifactRepository,
                         ISearchService searchService,
                         IIndexService indexService,
                         IExportService exportService) {
        this.projectService = projectService;
        this.templateService = templateService;
        this.viewManager = viewManager;
        this.projectStateService = projectStateService;
        this.artifactRepository = artifactRepository;
        this.searchService = searchService;
        this.indexService = indexService;
        this.exportService = exportService;

        this.projectRoot = new SimpleObjectProperty<>(new TreeItem<>("Chưa mở dự án"));
        this.statusMessage = new SimpleStringProperty("Sẵn sàng.");
        this.currentProject = new SimpleObjectProperty<>(null);

        this.currentProjectDirectory = new SimpleObjectProperty<>(null);

        projectStateService.statusMessageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && newMsg.startsWith("Đã lưu")) {
                refreshProjectTree();
            }
        });
    }

    /**
     * Hàm refresh cây thư mục (TreeView).
     */
    private void refreshProjectTree() {
        // ... (Không thay đổi) ...
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
     * @return Danh sách (Observable) các backlinks
     */
    public ObservableList<String> getCurrentBacklinks() {
        return currentBacklinks;
    }

    /**
     * Logic cập nhật Backlinks khi đổi Tab
     *
     * @param selectedTab Tab hiện tại đang được chọn
     */
    public void updateBacklinks(Tab selectedTab) {
        // ... (Cập nhật để bỏ qua tab mới) ...
        currentBacklinks.clear();
        if (selectedTab == null || "Welcome".equals(selectedTab.getText())) {
            currentBacklinks.add("(Chọn một artifact để xem)");
            return;
        }
        String artifactId = selectedTab.getText();
        if (artifactId.startsWith("New ") || artifactId.startsWith("Form Builder") || artifactId.startsWith("Releases Config") || artifactId.startsWith("Dashboard") || artifactId.startsWith("Export Templates")) {
            currentBacklinks.add("(Không áp dụng)");
            return;
        }
        try {
            List<Artifact> backlinks = searchService.getBacklinks(artifactId);
            if (backlinks.isEmpty()) {
                currentBacklinks.add("(Không có liên kết ngược nào)");
            } else {
                for (Artifact backlink : backlinks) {
                    currentBacklinks.add(backlink.getId() + ": " + backlink.getName());
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi tải backlinks cho {}: {}", artifactId, e.getMessage());
            currentBacklinks.add("(Lỗi khi tải backlinks)");
        }
    }

    /**
     * Logic nghiệp vụ cho UC-PM-01
     *
     * @param projectName Tên dự án
     * @param directory Thư mục
     */
    public void createNewProject(String projectName, File directory) {
        // ... (Không thay đổi) ...
        try {
            boolean success = projectService.createProject(projectName, directory);
            if (success) {
                openProject(directory);
                statusMessage.set("Tạo dự án mới thành công: " + projectName);
            }
        } catch (IOException e) {
            logger.error("Lỗi tạo dự án", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Logic cho "New Artifact"
     *
     * @param templateName Tên template (ví dụ: "Use Case")
     */
    public void createNewArtifact(String templateName) {
        // ... (Không thay đổi) ...
        try {
            ArtifactTemplate template = templateService.loadTemplate(templateName);
            Tab newTab = viewManager.openArtifactTab(template);
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Không thể tải template: " + templateName, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Logic mở Artifact đã tồn tại
     *
     * @param relativePath Đường dẫn tương đối (ví dụ: "UC/UC001.json")
     */
    public void openArtifact(String relativePath) {
        // ... (Không thay đổi) ...
        if (relativePath == null || !relativePath.endsWith(".json")) {
            logger.warn("Bỏ qua mở file không hợp lệ: {}", relativePath);
            return;
        }
        String id = new File(relativePath).getName().replace(".json", "");
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals(id)) {
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
            ArtifactTemplate template = templateService.loadTemplateByPrefix(artifact.getArtifactType());
            if (template == null) {
                throw new IOException("Không tìm thấy template cho loại: " + artifact.getArtifactType());
            }
            Tab newTab = viewManager.openArtifactTab(artifact, template);
            this.mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            logger.error("Lỗi mở artifact: " + relativePath, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Logic nghiệp vụ Xóa Artifact
     *
     * @param relativePath Đường dẫn tương đối của file cần xóa
     * @throws IOException Nếu xóa thất bại
     */
    public void deleteArtifact(String relativePath) throws IOException {
        // ... (Không thay đổi) ...
        artifactRepository.delete(relativePath);
        refreshProjectTree();
        String id = new File(relativePath).getName().replace(".json", "");
        Tab tabToClose = null;
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals(id)) {
                tabToClose = tab;
                break;
            }
        }
        if (tabToClose != null) {
            mainTabPane.getTabs().remove(tabToClose);
        }
        projectStateService.setStatusMessage("Đã xóa: " + id);
    }


    /**
     * Logic nghiệp vụ cho UC-PM-02
     *
     * @param directory Thư mục dự án
     */
    public void openProject(File directory) {
        // ... (Không thay đổi) ...
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
            indexService.validateAndRebuildIndex();
            refreshProjectTree();
            projectStateService.setStatusMessage("Đã mở dự án: " + config.getProjectName());
        } catch (IOException e) {
            logger.error("Lỗi mở dự án", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Mở tab Cấu hình Form Builder (UC-CFG-01).
     */
    public void openFormBuilderTab() {
        // ... (Không thay đổi) ...
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
        // ... (Không thay đổi) ...
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
        // ... (Không thay đổi) ...
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
     * Logic nghiệp vụ cho UC-PUB-02 (Xuất Excel).
     */
    public void exportProjectToExcel() {
        // ... (Không thay đổi) ...
        if (mainTabPane == null || mainTabPane.getScene() == null || mainTabPane.getScene().getWindow() == null) {
            projectStateService.setStatusMessage("Lỗi: Không thể mở hộp thoại lưu file.");
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
                    projectStateService.setStatusMessage("Không có loại (template) nào để xuất.");
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
                projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
            }
        }
    }

    /**
     * [THÊM MỚI NGÀY 32]
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


    public ObjectProperty<TreeItem<String>> projectRootProperty() {
        return projectRoot;
    }
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public void setMainTabPane(TabPane mainTabPane) {
        this.mainTabPane = mainTabPane;
    }
}