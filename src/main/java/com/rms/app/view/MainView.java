package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.service.IViewManager;
import com.rms.app.viewmodel.MainViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ITemplateService;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainView {

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

        mainTabPane.getTabs().setAll(viewModel.getOpenTabs());
        viewModel.getOpenTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    mainTabPane.getTabs().addAll(c.getAddedSubList());
                }
                if (c.wasRemoved()) {
                    mainTabPane.getTabs().removeAll(c.getRemoved());
                }
            }
        });

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

        // Thêm vào Accordion ở Cột Phải
        rightAccordion.getPanes().add(backlinksPane);
        rightAccordion.setExpandedPane(backlinksPane); // Mở sẵn pane này

        // Thêm listener lắng nghe sự kiện đổi tab
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            viewModel.updateBacklinks(newTab);
        });

        // Cập nhật cho tab "Welcome" (tab đầu tiên)
        viewModel.updateBacklinks(mainTabPane.getSelectionModel().getSelectedItem());

        // Thêm listener double-click để điều hướng (Giống UC-DEV-02)
        backlinksListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                String selected = backlinksListView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.contains(":")) {
                    String artifactId = selected.split(":")[0].trim();
                    // Gọi hàm openArtifact (cần thêm .json để khớp logic)
                    viewModel.openArtifact(artifactId + ".json");
                }
            }
        });
    }

    private void setupTreeViewClickListener() {
        projectTreeView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.isLeaf()) {
                    String fileName = selectedItem.getValue();
                    if (fileName.endsWith(".json")) {
                        viewModel.openArtifact(fileName);
                    }
                }
            }
        });
    }

    private void setupTreeViewContextMenu() {
        ContextMenu treeContextMenu = new ContextMenu();

        Menu newMenu = new Menu("New Artifact");

        projectStateService.currentProjectDirectoryProperty().addListener((obs, oldDir, newDir) -> {
            newMenu.getItems().clear();
            if (newDir != null) {
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
        });

        newMenu.getItems().add(new MenuItem("(Mở dự án để xem template)"));
        treeContextMenu.getItems().add(newMenu);

        projectTreeView.setContextMenu(treeContextMenu);
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