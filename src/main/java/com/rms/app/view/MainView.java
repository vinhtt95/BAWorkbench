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
import com.rms.app.service.ITemplateService; // Thêm import
import java.io.File;
import java.io.IOException; // Thêm import
import java.util.List; // Thêm import

public class MainView {

    @FXML private TreeView<String> projectTreeView;
    @FXML private TabPane mainTabPane;
    @FXML private Accordion rightAccordion;
    @FXML private Label statusLabel;

    private final MainViewModel viewModel;
    private final IProjectStateService projectStateService;
    private final ITemplateService templateService; // Thêm service

    @Inject
    public MainView(MainViewModel viewModel, IProjectStateService projectStateService, ITemplateService templateService) { // Cập nhật constructor
        this.viewModel = viewModel;
        this.projectStateService = projectStateService;
        this.templateService = templateService; // Thêm service
    }

    @FXML
    public void initialize() {
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

        System.out.println("MainView initialized. ViewModel is: " + viewModel);
    }

    private void setupTreeViewContextMenu() {
        ContextMenu treeContextMenu = new ContextMenu();

        Menu newMenu = new Menu("New Artifact");

        /**
         * [SỬA LỖI 3] Đọc template động thay vì hard-code
         * Thêm listener để menu tự cập nhật khi dự án được mở
         */
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

        /**
         * Kích hoạt lần đầu (trường hợp rỗng)
         */
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