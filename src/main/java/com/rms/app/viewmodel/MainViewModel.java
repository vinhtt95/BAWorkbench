package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.ProjectConfig;
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
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MainViewModel {

    private static final Logger logger = LoggerFactory.getLogger(MainViewModel.class);
    private final IProjectService projectService;

    // --- Properties cho View Binding ---
    private final ObjectProperty<TreeItem<String>> projectRoot;
    private final ObservableList<Tab> openTabs;
    private final StringProperty statusMessage;
    private final ObjectProperty<ProjectConfig> currentProject;

    // SỬA LỖI: Thêm property để lưu trữ thư mục gốc
    private final ObjectProperty<File> currentProjectDirectory;
    private final ITemplateService templateService;
    private final IViewManager viewManager;
    private final IProjectStateService projectStateService;

    @Inject
    public MainViewModel(IProjectService projectService,
                         ITemplateService templateService,
                         IViewManager viewManager,
                         IProjectStateService projectStateService) {
        this.projectService = projectService;
        this.templateService = templateService;
        this.viewManager = viewManager;
        this.projectStateService = projectStateService;

        // Khởi tạo
        this.projectRoot = new SimpleObjectProperty<>(new TreeItem<>("Chưa mở dự án"));
        this.openTabs = FXCollections.observableArrayList();
        this.statusMessage = new SimpleStringProperty("Sẵn sàng.");
        this.currentProject = new SimpleObjectProperty<>(null);

        // SỬA LỖI: Khởi tạo property
        this.currentProjectDirectory = new SimpleObjectProperty<>(null);

        Tab welcomeTab = new Tab("Welcome");
        welcomeTab.setContent(new javafx.scene.control.Label("Chào mừng đến với RMS v1.0"));
        this.openTabs.add(welcomeTab);
    }

    /**
     * Logic nghiệp vụ cho UC-PM-01
     */
    public void createNewProject(String projectName, File directory) {
        try {
            boolean success = projectService.createProject(projectName, directory);
            if (success) {
                // SỬA LỖI: Lưu lại thư mục dự án
                this.currentProjectDirectory.set(directory);
                openProject(directory);
                statusMessage.set("Tạo dự án mới thành công: " + projectName);
            }
        } catch (IOException e) {
            logger.error("Lỗi tạo dự án", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    // Logic cho "New Artifact" (Ngày 10)
    public void createNewArtifact(String templateName) {
        try {
            // 2.0. Hệ thống đọc file cấu hình template
            ArtifactTemplate template = templateService.loadTemplate(templateName);
            Tab newTab = viewManager.openArtifactTab(template);
            this.openTabs.add(newTab);

        } catch (IOException e) {
            logger.error("Không thể tải template: " + templateName, e);
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

            TreeItem<String> rootNode = projectService.buildProjectTree(directory);
            projectRoot.set(rootNode);

            projectStateService.setStatusMessage("Đã mở dự án: " + config.getProjectName());
        } catch (IOException e) {
            logger.error("Lỗi mở dự án", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }


    // --- Getters (và Properties) cho View binding ---

    // SỬA LỖI: Getter cho Repository sử dụng
    public File getCurrentProjectDirectory() {
        return currentProjectDirectory.get();
    }

    // THÊM MỚI: Logic mở Form Builder (do MainView gọi)
    public void openFormBuilderTab() {
        try {
            Tab newTab = viewManager.openViewInNewTab(
                    "/com/rms/app/view/FormBuilderView.fxml", "Form Builder"
            );
            this.openTabs.add(newTab);
        } catch (IOException e) {
            // Lỗi đã được log bởi ViewManager
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
}