package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.ProjectConfig;
import com.rms.app.service.IArtifactRepository;
import com.rms.app.service.IProjectService;
import com.rms.app.service.ITemplateService;
import com.rms.app.service.IViewManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import com.rms.app.service.IProjectStateService;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MainViewModel {

    private static final Logger logger = LoggerFactory.getLogger(MainViewModel.class);
    private final IProjectService projectService;

    private final ObjectProperty<TreeItem<String>> projectRoot;
    private final ObservableList<Tab> openTabs;
    private final StringProperty statusMessage;
    private final ObjectProperty<ProjectConfig> currentProject;

    private final ObjectProperty<File> currentProjectDirectory;
    private final ITemplateService templateService;
    private final IViewManager viewManager;
    private final IProjectStateService projectStateService;
    private final IArtifactRepository artifactRepository;

    @Inject
    public MainViewModel(IProjectService projectService,
                         ITemplateService templateService,
                         IViewManager viewManager,
                         IProjectStateService projectStateService,
                         IArtifactRepository artifactRepository) {
        this.projectService = projectService;
        this.templateService = templateService;
        this.viewManager = viewManager;
        this.projectStateService = projectStateService;
        this.artifactRepository = artifactRepository;

        this.projectRoot = new SimpleObjectProperty<>(new TreeItem<>("Chưa mở dự án"));
        this.openTabs = FXCollections.observableArrayList();
        this.statusMessage = new SimpleStringProperty("Sẵn sàng.");
        this.currentProject = new SimpleObjectProperty<>(null);

        this.currentProjectDirectory = new SimpleObjectProperty<>(null);

        Tab welcomeTab = new Tab("Welcome");
        welcomeTab.setContent(new javafx.scene.control.Label("Chào mừng đến với RMS v1.0"));
        this.openTabs.add(welcomeTab);

        /**
         * Thêm listener để tự động refresh TreeView khi có file mới.
         */
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
     * Logic nghiệp vụ cho UC-PM-01
     */
    public void createNewProject(String projectName, File directory) {
        try {
            boolean success = projectService.createProject(projectName, directory);
            if (success) {
                this.currentProjectDirectory.set(directory);
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
     */
    public void createNewArtifact(String templateName) {
        try {
            ArtifactTemplate template = templateService.loadTemplate(templateName);

            /**
             * [SỬA LỖI UX] Khi tạo mới, hệ thống cũng nên kiểm tra
             * các tab đã mở để tránh trùng lặp. Tuy nhiên, logic
             * này thường phức tạp hơn (ví dụ: mở file mới có tên trùng)
             * nên tạm thời ta chỉ tập trung vào openArtifact.
             */
            Tab newTab = viewManager.openArtifactTab(template);
            this.openTabs.add(newTab);
            mainTabPane.getSelectionModel().select(newTab);

        } catch (IOException e) {
            logger.error("Không thể tải template: " + templateName, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Logic mở Artifact đã tồn tại
     */
    public void openArtifact(String fileName) {
        if (fileName == null || !fileName.endsWith(".json")) {
            logger.warn("Bỏ qua mở file không hợp lệ: {}", fileName);
            return;
        }

        String id = fileName.replace(".json", "");

        /**
         * [SỬA LỖI UX] Kiểm tra xem tab đã mở chưa
         */
        for (Tab tab : openTabs) {
            if (tab.getText().equals(id)) {
                mainTabPane.getSelectionModel().select(tab);
                logger.info("Tab cho {} đã mở, chuyển sang tab này.", id);
                return;
            }
        }

        try {
            /**
             * 1. Load dữ liệu artifact
             */
            Artifact artifact = artifactRepository.load(id);
            if (artifact == null) {
                throw new IOException("Không tìm thấy artifact: " + id);
            }

            /**
             * 2. Load template (form) tương ứng
             */
            ArtifactTemplate template = templateService.loadTemplateByPrefix(artifact.getArtifactType());
            if (template == null) {
                throw new IOException("Không tìm thấy template cho loại: " + artifact.getArtifactType());
            }

            /**
             * 3. Mở tab
             */
            Tab newTab = viewManager.openArtifactTab(artifact, template);
            this.openTabs.add(newTab);
            mainTabPane.getSelectionModel().select(newTab);

        } catch (IOException e) {
            logger.error("Lỗi mở artifact: " + fileName, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Logic nghiệp vụ cho UC-PM-02
     */
    public void openProject(File directory) {
        try {
            ProjectConfig config = projectService.openProject(directory);
            currentProject.set(config);

            projectStateService.setCurrentProjectDirectory(directory);

            refreshProjectTree();

            projectStateService.setStatusMessage("Đã mở dự án: " + config.getProjectName());
        } catch (IOException e) {
            logger.error("Lỗi mở dự án", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }


    public File getCurrentProjectDirectory() {
        return currentProjectDirectory.get();
    }

    public void openFormBuilderTab() {
        try {
            Tab newTab = viewManager.openViewInNewTab(
                    "/com/rms/app/view/FormBuilderView.fxml", "Form Builder"
            );
            this.openTabs.add(newTab);
        } catch (IOException e) {
            /**
             * Lỗi đã được log bởi ViewManager
             */
        }
    }

    public ObjectProperty<TreeItem<String>> projectRootProperty() {
        return projectRoot;
    }
    public ObservableList<Tab> getOpenTabs() {
        return openTabs;
    }
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    private TabPane mainTabPane;
    public void setMainTabPane(TabPane mainTabPane) {
        this.mainTabPane = mainTabPane;
    }
}