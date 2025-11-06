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
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    private final ObservableList<String> currentBacklinks = FXCollections.observableArrayList();

    @Inject
    public MainViewModel(IProjectService projectService,
                         ITemplateService templateService,
                         IViewManager viewManager,
                         IProjectStateService projectStateService,
                         IArtifactRepository artifactRepository,
                         ISearchService searchService,
                         IIndexService indexService) {
        this.projectService = projectService;
        this.templateService = templateService;
        this.viewManager = viewManager;
        this.projectStateService = projectStateService;
        this.artifactRepository = artifactRepository;
        this.searchService = searchService;
        this.indexService = indexService;

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
     */
    public ObservableList<String> getCurrentBacklinks() {
        return currentBacklinks;
    }

    /**
     * Logic cập nhật Backlinks khi đổi Tab
     * (Sẽ được gọi bởi MainView)
     */
    public void updateBacklinks(Tab selectedTab) {
        currentBacklinks.clear();
        if (selectedTab == null || "Welcome".equals(selectedTab.getText())) {
            currentBacklinks.add("(Chọn một artifact để xem)");
            return;
        }

        String artifactId = selectedTab.getText();
        if (artifactId.startsWith("New ") || artifactId.startsWith("Form Builder")) {
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
     */
    public void createNewProject(String projectName, File directory) {
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
     * [THÊM MỚI NGÀY 23] Logic nghiệp vụ Xóa Artifact
     *
     * @param relativePath Đường dẫn tương đối của file cần xóa
     * @throws IOException Nếu xóa thất bại (ví dụ: do vi phạm toàn vẹn)
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


    public File getCurrentProjectDirectory() {
        return currentProjectDirectory.get();
    }

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

    public ObjectProperty<TreeItem<String>> projectRootProperty() {
        return projectRoot;
    }
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    private TabPane mainTabPane;
    public void setMainTabPane(TabPane mainTabPane) {
        this.mainTabPane = mainTabPane;
    }
}