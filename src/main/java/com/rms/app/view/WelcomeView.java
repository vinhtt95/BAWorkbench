package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.RecentProject;
import com.rms.app.service.IViewManager;
import com.rms.app.viewmodel.MainViewModel;
import com.rms.app.viewmodel.WelcomeViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;

public class WelcomeView {

    @FXML private ListView<RecentProject> recentProjectsList;

    private final WelcomeViewModel viewModel;
    private final MainViewModel mainViewModel; // Để gọi logic mở dự án chính
    private final IViewManager viewManager; // Để chuyển cảnh

    @Inject
    public WelcomeView(WelcomeViewModel viewModel, MainViewModel mainViewModel, IViewManager viewManager) {
        this.viewModel = viewModel;
        this.mainViewModel = mainViewModel;
        this.viewManager = viewManager;
    }

    @FXML
    public void initialize() {
        // Custom Cell để hiển thị đẹp hơn (Tên dự án đậm, đường dẫn nhạt)
        recentProjectsList.setCellFactory(new Callback<>() {
            @Override
            public ListCell<RecentProject> call(ListView<RecentProject> list) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(RecentProject item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            VBox container = new VBox(2);
                            Label nameLabel = new Label(item.getName());
                            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E3E3E3; -fx-font-size: 14px;");

                            Label pathLabel = new Label(item.getPath());
                            pathLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

                            container.getChildren().addAll(nameLabel, pathLabel);
                            setGraphic(container);
                            setStyle("-fx-padding: 5 10 5 10;");
                        }
                    }
                };
            }
        });

        // Xử lý click vào dự án gần đây
        recentProjectsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                RecentProject selected = recentProjectsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openSelectedProject(new File(selected.getPath()));
                }
            }
        });

        recentProjectsList.setItems(viewModel.recentProjects);
        viewModel.loadRecentProjects();
    }

    @FXML
    private void handleNewProject() {
        // Logic hiển thị Form nhập thông tin dự án mới (Sẽ làm ở bước sau)
        // Tạm thời dùng dialog cũ
        TextInputDialog nameDialog = new TextInputDialog("NewProject");
        nameDialog.setTitle("New Project");
        nameDialog.setHeaderText("Nhập tên dự án:");
        nameDialog.showAndWait().ifPresent(projectName -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Chọn vị trí lưu dự án");
            File location = directoryChooser.showDialog(recentProjectsList.getScene().getWindow());
            if (location != null) {
                File projectDir = new File(location, projectName);
                mainViewModel.createNewProject(projectName, projectDir);
                viewModel.addToRecent(projectDir);
                switchToMainView();
            }
        });
    }

    @FXML
    private void handleOpenProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Mở dự án RMS");
        File selectedDirectory = directoryChooser.showDialog(recentProjectsList.getScene().getWindow());
        if (selectedDirectory != null) {
            openSelectedProject(selectedDirectory);
        }
    }

    private void openSelectedProject(File projectDir) {
        if (projectDir.exists()) {
            mainViewModel.openProject(projectDir);
            viewModel.addToRecent(projectDir);
            switchToMainView();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Thư mục dự án không còn tồn tại!");
            alert.show();
        }
    }

    @FXML
    private void handleCloneProject() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Tính năng Clone từ Git đang được phát triển.");
        alert.show();
    }

    private void switchToMainView() {
        // Logic chuyển cảnh sang MainView
        // Cần gọi ViewManager để thay đổi Scene của Stage chính
        Stage stage = (Stage) recentProjectsList.getScene().getWindow();
        try {
            // Giả sử ViewManager có hàm để switch scene, hoặc ta tự load lại
            // Ở đây ta dùng viewManager để mở MainView
            // Lưu ý: Cần update Interface IViewManager để hỗ trợ việc này
            viewManager.showMainView(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}