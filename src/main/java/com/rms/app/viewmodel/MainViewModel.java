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

    @Inject
    public MainViewModel(IProjectService projectService,
                         ITemplateService templateService,
                         IViewManager viewManager) {
        this.projectService = projectService;
        this.templateService = templateService;
        this.viewManager = viewManager;

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
            statusMessage.set("Lỗi: " + e.getMessage());
        }
    }

    // Logic cho "New Artifact" (Ngày 10)
    public void createNewArtifact(String templateName) {
        try {
            // 2.0. Hệ thống đọc file cấu hình template
            ArtifactTemplate template = templateService.loadTemplate(templateName);

            // Yêu cầu ViewManager mở tab mới với template này
            viewManager.openArtifactTab(template);

        } catch (IOException e) {
            logger.error("Không thể tải template: " + templateName, e);
            statusMessage.set("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Logic nghiệp vụ cho UC-PM-02
     */
    public void openProject(File directory) {
        try {
            ProjectConfig config = projectService.openProject(directory);
            currentProject.set(config);

            // SỬA LỖI: Lưu lại thư mục dự án
            this.currentProjectDirectory.set(directory);

            TreeItem<String> rootNode = projectService.buildProjectTree(directory);
            projectRoot.set(rootNode);

            statusMessage.set("Đã mở dự án: " + config.getProjectName());
        } catch (IOException e) {
            logger.error("Lỗi mở dự án", e);
            statusMessage.set("Lỗi: " + e.getMessage());
        }
    }


    // --- Getters (và Properties) cho View binding ---

    // SỬA LỖI: Getter cho Repository sử dụng
    public File getCurrentProjectDirectory() {
        return currentProjectDirectory.get();
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