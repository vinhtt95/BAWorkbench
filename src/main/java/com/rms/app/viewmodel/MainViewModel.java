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
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "Brain" - Logic UI cho MainView.
 * Quản lý trạng thái chung của ứng dụng.
 */
public class MainViewModel {

    private static final Logger logger = LoggerFactory.getLogger(MainViewModel.class);
    private final IProjectService projectService;

    private final ObjectProperty<TreeItem<String>> projectRoot;
    // [SỬA LỖI] Xóa 'statusMessage' cục bộ
    // private final StringProperty statusMessage;
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
        // [SỬA LỖI] Xóa khởi tạo 'statusMessage'
        // this.statusMessage = new SimpleStringProperty("Sẵn sàng.");
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
        try {
            boolean success = projectService.createProject(projectName, directory);
            if (success) {
                openProject(directory);
                /**
                 * [SỬA LỖI] Cập nhật vào projectStateService
                 */
                projectStateService.setStatusMessage("Tạo dự án mới thành công: " + projectName);
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
     * Logic nghiệp vụ cho UC-PUB-02 (Xuất Excel).
     */
    public void exportProjectToExcel() {
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
            projectStateService.setStatusMessage("Lỗi: Vui lòng mở một dự án trước.");
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
                projectStateService.setStatusMessage("Lỗi: Không tìm thấy 'Template Xuất bản' (UC-CFG-03).");
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
                    projectStateService.setStatusMessage("Lỗi: Bạn phải chọn một template.");
                    return;
                }

                /**
                 * Chuyển đổi "REL001: V1.0" -> "REL001"
                 * hoặc "Không lọc" -> null
                 */
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
                            projectStateService.setStatusMessage("Lỗi Xuất bản: " + getException().getMessage());
                            logger.error("Lỗi Xuất bản (UC-PUB-01)", getException());
                        }
                    };
                    new Thread(exportTask).start();
                }
            }

        } catch (Exception e) {
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
            logger.error("Không thể mở Dialog Xuất bản", e);
        }
    }


    public ObjectProperty<TreeItem<String>> projectRootProperty() {
        return projectRoot;
    }

    // [SỬA LỖI] Xóa phương thức này
    // public StringProperty statusMessageProperty() {
    //    return statusMessage;
    // }

    public void setMainTabPane(TabPane mainTabPane) {
        this.mainTabPane = mainTabPane;
    }
}