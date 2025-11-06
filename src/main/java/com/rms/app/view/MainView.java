package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainView {

    // 1. FXML Controls
    @FXML private TreeView<String> projectTreeView;
    @FXML private TabPane mainTabPane;
    @FXML private Accordion rightAccordion;
    @FXML private Label statusLabel;

    // 2. ViewModel
    private final MainViewModel viewModel;

    @Inject
    public MainView(MainViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // 3. Initialize
    @FXML
    public void initialize() {
        // Binding
        projectTreeView.rootProperty().bind(viewModel.projectRootProperty());
        mainTabPane.getTabs().setAll(viewModel.getOpenTabs()); // Khởi tạo lần đầu
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        System.out.println("MainView initialized. ViewModel is: " + viewModel);
    }

    // --- Event Handlers (Gọi ViewModel) ---

    @FXML
    private void handleNewProject() {
        // 2.0. Hệ thống hiển thị cửa sổ
        // (Sử dụng Dialog đơn giản)
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
        // 2.0. Hệ thống hiển thị một cửa sổ chọn thư mục
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open RMS Project");
        File selectedDirectory = directoryChooser.showDialog(getStage());

        if (selectedDirectory != null) {
            // 4.0. BA nhấn "Open" (logic được xử lý bởi showDialog)
            viewModel.openProject(selectedDirectory);
        }
    }

    private Stage getStage() {
        return (Stage) statusLabel.getScene().getWindow();
    }
}