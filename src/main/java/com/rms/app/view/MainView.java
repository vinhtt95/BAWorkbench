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
import java.util.Optional;

/**
 * "Dumb" View Controller cho MainView.fxml.
 * Chịu trách nhiệm binding dữ liệu và chuyển tiếp sự kiện
 * (ví dụ: click) đến MainViewModel.
 */
public class MainView {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    @FXML private TreeView<String> projectTreeView;
    @FXML private TabPane mainTabPane;
    @FXML private Accordion rightAccordion;
    @FXML private Label statusLabel;

    @FXML private TitledPane backlinksPane;
    @FXML private ListView<String> backlinksListView;

    private final MainViewModel viewModel;
    private final IProjectStateService projectStateService;
    private final ITemplateService templateService;

    @Inject
    public MainView(MainViewModel viewModel, IProjectStateService projectStateService, ITemplateService templateService) {
        this.viewModel = viewModel;
        this.projectStateService = projectStateService;
        this.templateService = templateService;
    }

    /**
     * Khởi tạo Controller.
     */
    @FXML
    public void initialize() {
        viewModel.setMainTabPane(mainTabPane);

        projectTreeView.rootProperty().bind(viewModel.projectRootProperty());
        statusLabel.textProperty().bind(projectStateService.statusMessageProperty());

        Tab welcomeTab = new Tab("Welcome");
        welcomeTab.setContent(new javafx.scene.control.Label("Chào mừng đến với RMS v1.0"));
        mainTabPane.getTabs().add(welcomeTab);

        setupTreeViewContextMenu();
        setupTreeViewClickListener();
        setupBacklinksPanel();

        System.out.println("MainView initialized. ViewModel is: " + viewModel);
    }

    /**
     * Thiết lập logic UI cho bảng Backlinks (Cột phải).
     */
    private void setupBacklinksPanel() {
        backlinksListView.setItems(viewModel.getCurrentBacklinks());
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
                    viewModel.openArtifact(artifactId + ".json");
                }
            }
        });
    }

    /**
     * Thiết lập trình lắng nghe sự kiện click chuột cho TreeView (Cột trái).
     */
    private void setupTreeViewClickListener() {
        projectTreeView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.isLeaf()) {
                    String fileName = selectedItem.getValue();
                    if (fileName.endsWith(".json")) {
                        String relativePath = getSelectedArtifactPath(selectedItem);
                        if (relativePath != null) {
                            logger.info("Đang mở artifact: " + relativePath);
                            viewModel.openArtifact(relativePath);
                        }
                    }
                }
            }
        });
    }

    /**
     * Thiết lập Context Menu (menu chuột phải) cho TreeView.
     */
    private void setupTreeViewContextMenu() {
        ContextMenu treeContextMenu = new ContextMenu();
        Menu newMenu = new Menu("New Artifact");
        projectStateService.currentProjectDirectoryProperty().addListener((obs, oldDir, newDir) -> {
            rebuildNewArtifactMenu(newMenu);
        });
        projectStateService.statusMessageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && newMsg.startsWith("Đã lưu template")) {
                rebuildNewArtifactMenu(newMenu);
            }
        });
        rebuildNewArtifactMenu(newMenu);
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> handleDeleteArtifact());
        treeContextMenu.getItems().addAll(newMenu, new SeparatorMenuItem(), deleteItem);
        projectTreeView.setContextMenu(treeContextMenu);
    }

    /**
     * Xử lý sự kiện khi người dùng chọn "Delete" từ Context Menu.
     */
    @FXML
    private void handleDeleteArtifact() {
        TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || !selectedItem.isLeaf() || !selectedItem.getValue().endsWith(".json")) {
            showErrorAlert("Lỗi Xóa", "Vui lòng chọn một file artifact (.json) để xóa.");
            return;
        }
        String relativePath = getSelectedArtifactPath(selectedItem);
        if (relativePath == null) return;
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận Xóa");
        confirmAlert.setHeaderText("Bạn có chắc muốn xóa " + selectedItem.getValue() + "?");
        confirmAlert.setContentText("Hành động này không thể hoàn tác.");
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                viewModel.deleteArtifact(relativePath);
            } catch (IOException e) {
                showErrorAlert("Lỗi Toàn vẹn Dữ liệu", e.getMessage());
            }
        }
    }

    /**
     * Hàm helper để xây dựng lại (rebuild) menu "New Artifact" một cách động.
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

    /**
     * Hàm helper lấy đường dẫn tương đối của artifact từ TreeItem được chọn.
     *
     * @param selectedItem Mục (item) được chọn trong TreeView
     * @return Đường dẫn tương đối (ví dụ: "UC/UC001.json") hoặc null
     */
    private String getSelectedArtifactPath(TreeItem<String> selectedItem) {
        if (selectedItem == null || !selectedItem.isLeaf() || !selectedItem.getValue().endsWith(".json")) {
            return null;
        }
        String relativePath = selectedItem.getValue();
        TreeItem<String> parent = selectedItem.getParent();
        while (parent != null && !parent.getValue().equals(ProjectServiceImpl.ARTIFACTS_DIR) && !parent.getValue().equals(projectTreeView.getRoot().getValue())) {
            relativePath = parent.getValue() + File.separator + relativePath;
            parent = parent.getParent();
        }
        return relativePath;
    }

    /**
     * Hàm helper hiển thị Alert Lỗi cho người dùng.
     *
     * @param title   Tiêu đề của Alert
     * @param content Nội dung lỗi
     */
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Xử lý sự kiện nhấn "File > New Project...".
     */
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

    /**
     * Xử lý sự kiện nhấn "File > Open Project...".
     */
    @FXML
    private void handleOpenProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open RMS Project");
        File selectedDirectory = directoryChooser.showDialog(getStage());
        if (selectedDirectory != null) {
            viewModel.openProject(selectedDirectory);
        }
    }

    /**
     * Xử lý sự kiện nhấn "Settings > Artifact Types (Form Builder)".
     */
    @FXML
    private void handleOpenFormBuilder() {
        viewModel.openFormBuilderTab();
    }

    /**
     * Xử lý sự kiện nhấn "Settings > Releases Management...".
     */
    @FXML
    private void handleOpenReleasesConfig() {
        viewModel.openReleasesConfigTab();
    }

    /**
     * Xử lý sự kiện nhấn "View > Planning Dashboard".
     */
    @FXML
    private void handleOpenDashboard() {
        viewModel.openDashboardTab();
    }

    /**
     * Xử lý sự kiện nhấn "File > Export to Excel...".
     */
    @FXML
    private void handleExportExcel() {
        viewModel.exportProjectToExcel();
    }

    /**
     * Xử lý sự kiện nhấn "Settings > Export Template Builder...".
     * Tuân thủ UC-CFG-03.
     */
    @FXML
    private void handleOpenExportTemplateBuilder() {
        viewModel.openExportTemplateBuilderTab();
    }

    /**
     * Xử lý sự kiện nhấn "File > Export to Document..."
     * Tuân thủ UC-PUB-01 (Ngày 33).
     */
    @FXML
    private void handleExportToDocument() {
        viewModel.openExportToDocumentDialog();
    }

    /**
     * [THÊM MỚI] Xử lý sự kiện nhấn "File > Import from Excel..."
     * Tuân thủ UC-PM-03 (Ngày 34).
     */
    @FXML
    private void handleImportExcel() {
        viewModel.openImportWizardTab();
    }

    /**
     * Hàm helper lấy Stage (cửa sổ) chính của ứng dụng.
     * @return Stage chính
     */
    private Stage getStage() {
        return (Stage) statusLabel.getScene().getWindow();
    }
}