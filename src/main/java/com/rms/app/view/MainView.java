package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.service.IViewManager;
import com.rms.app.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ITemplateService;
import com.rms.app.service.impl.ProjectServiceImpl;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainView {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    @FXML private TreeView<String> projectTreeView;
    @FXML private TabPane mainTabPane;
    @FXML private Accordion rightAccordion;
    @FXML private Label statusLabel;

    private final MainViewModel viewModel;
    private final IProjectStateService projectStateService;
    private final ITemplateService templateService;

    @Inject
    public MainView(MainViewModel viewModel, IProjectStateService projectStateService, ITemplateService templateService) {
        this.viewModel = viewModel;
        this.projectStateService = projectStateService;
        this.templateService = templateService;
    }

    @FXML
    public void initialize() {
        viewModel.setMainTabPane(mainTabPane);

        projectTreeView.rootProperty().bind(viewModel.projectRootProperty());
        statusLabel.textProperty().bind(projectStateService.statusMessageProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        Tab welcomeTab = new Tab("Welcome");
        welcomeTab.setContent(new javafx.scene.control.Label("Chào mừng đến với RMS v1.0"));
        mainTabPane.getTabs().add(welcomeTab);

        setupTreeViewContextMenu();
        setupTreeViewClickListener();
        setupBacklinksPanel();

        System.out.println("MainView initialized. ViewModel is: " + viewModel);
    }

    /**
     * Triển khai UI cho Backlinks
     */
    private void setupBacklinksPanel() {
        ListView<String> backlinksListView = new ListView<>();
        backlinksListView.setItems(viewModel.getCurrentBacklinks());
        TitledPane backlinksPane = new TitledPane("Backlinks (Liên kết ngược)", backlinksListView);

        rightAccordion.getPanes().add(backlinksPane);
        rightAccordion.setExpandedPane(backlinksPane);

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            viewModel.updateBacklinks(newTab);
        });

        viewModel.updateBacklinks(mainTabPane.getSelectionModel().getSelectedItem());

        backlinksListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                String selected = backlinksListView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.contains(":")) {
                    String artifactId = selected.split(":")[0].trim();

                    logger.warn("Điều hướng Backlink chưa được triển khai đầy đủ để hỗ trợ cấu trúc thư mục con.");
                    // TODO: Logic điều hướng backlink cần biết đường dẫn tương đối,
                    // hiện tại chúng ta chỉ có ID. Tạm thời gọi hàm cũ.
                    viewModel.openArtifact(artifactId + ".json");
                }
            }
        });
    }

    /**
     * Cập nhật listener để xây dựng đường dẫn tương đối
     */
    private void setupTreeViewClickListener() {
        projectTreeView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();

                if (selectedItem != null && selectedItem.isLeaf()) {
                    String fileName = selectedItem.getValue();

                    if (fileName.endsWith(".json")) {

                        String relativePath = fileName;
                        TreeItem<String> parent = selectedItem.getParent();

                        while(parent != null && !parent.getValue().equals(ProjectServiceImpl.ARTIFACTS_DIR) && !parent.getValue().equals(projectTreeView.getRoot().getValue())) {
                            relativePath = parent.getValue() + File.separator + relativePath;
                            parent = parent.getParent();
                        }

                        logger.info("Đang mở artifact: " + relativePath);
                        viewModel.openArtifact(relativePath);
                    }
                }
            }
        });
    }

    /**
     * [SỬA ĐỔI] Tách logic build Context Menu
     */
    private void setupTreeViewContextMenu() {
        ContextMenu treeContextMenu = new ContextMenu();
        Menu newMenu = new Menu("New Artifact");

        /**
         * Listener 1: Cập nhật khi Mở/Đóng dự án
         */
        projectStateService.currentProjectDirectoryProperty().addListener((obs, oldDir, newDir) -> {
            rebuildNewArtifactMenu(newMenu);
        });

        /**
         * [SỬA LỖI 1] Listener 2: Cập nhật khi Template được lưu
         */
        projectStateService.statusMessageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && newMsg.startsWith("Đã lưu template")) {
                rebuildNewArtifactMenu(newMenu);
            }
        });

        rebuildNewArtifactMenu(newMenu);
        treeContextMenu.getItems().add(newMenu);

        projectTreeView.setContextMenu(treeContextMenu);
    }

    /**
     * [THÊM MỚI] Hàm helper để rebuild menu "New Artifact"
     *
     * @param newMenu Menu (MenuItem) cần được xây dựng lại
     */
    private void rebuildNewArtifactMenu(Menu newMenu) {
        newMenu.getItems().clear();
        if (projectStateService.getCurrentProjectDirectory() != null) {
            try {
                List<String> templateNames = templateService.loadAllTemplateNames();
                if (templateNames.isEmpty()) {
                    newMenu.getItems().add(new MenuItem("(Không tìm thấy template nào)"));
                } else {
                    for (String templateName : templateNames) {
                        MenuItem item = new MenuItem(templateName);
                        item.setOnAction(e -> viewModel.createNewArtifact(templateName));
                        newMenu.getItems().add(item);
                    }
                }
            } catch (IOException e) {
                newMenu.getItems().add(new MenuItem("(Lỗi tải template)"));
            }
        } else {
            newMenu.getItems().add(new MenuItem("(Mở dự án để xem template)"));
        }
    }


    @FXML
    private void handleNewProject() {
        TextInputDialog nameDialog = new TextInputDialog("MyProject");
        nameDialog.setTitle("New Project");
        nameDialog.setHeaderText("Enter Project Name:");
        nameDialog.showAndWait().ifPresent(projectName -> {

            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Project Location");
            File location = directoryChooser.showDialog(getStage());

            if (location != null && projectName != null && !projectName.isEmpty()) {
                File projectDir = new File(location, projectName);
                viewModel.createNewProject(projectName, projectDir);
            }
        });
    }

    @FXML
    private void handleOpenProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open RMS Project");
        File selectedDirectory = directoryChooser.showDialog(getStage());

        if (selectedDirectory != null) {
            viewModel.openProject(selectedDirectory);
        }
    }

    @FXML
    private void handleOpenFormBuilder() {
        viewModel.openFormBuilderTab();
    }

    private Stage getStage() {
        return (Stage) statusLabel.getScene().getWindow();
    }
}