package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.Artifact; // [MỚI] Import
import com.rms.app.service.IViewManager;
import com.rms.app.service.impl.ProjectServiceImpl; // [MỚI] Import
import com.rms.app.viewmodel.MainViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ITemplateService;
// [XÓA] Import này không còn cần thiết
// import com.rms.app.service.impl.ProjectServiceImpl;
import javafx.scene.input.*;
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
 * [CẬP NHẬT] Sửa lỗi Backlinks click và TreeView click.
 */
public class MainView {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    @FXML private TreeView<String> projectTreeView;
    @FXML private TabPane mainTabPane;
    @FXML private Accordion rightAccordion;
    @FXML private Label statusLabel;

    @FXML private TitledPane backlinksPane;
    /**
     * [SỬA LỖI] Thay đổi từ String sang Artifact
     */
    @FXML private ListView<Artifact> backlinksListView;

    private final MainViewModel viewModel;
    private final IProjectStateService projectStateService;
    private final ITemplateService templateService;
    private final IViewManager viewManager;

    /**
     * Biến (field) để theo dõi Tab đang được kéo (drag)
     */
    private Tab draggingTab = null;

    @Inject
    public MainView(MainViewModel viewModel,
                    IProjectStateService projectStateService,
                    ITemplateService templateService,
                    IViewManager viewManager) {
        this.viewModel = viewModel;
        this.projectStateService = projectStateService;
        this.templateService = templateService;
        this.viewManager = viewManager;
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

        /**
         * Kích hoạt (activate) logic kéo-thả (drag-and-drop)
         * cho các tab (UI-02)
         */
        setupTabDragAndDrop();
    }

    /**
     * Thiết lập logic Kéo-Thả (Drag-and-Drop) cho các tab (UI-02).
     */
    private void setupTabDragAndDrop() {
        /**
         * Lắng nghe các tab MỚI được thêm vào TabPane
         */
        mainTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab newTab : c.getAddedSubList()) {
                        attachDragHandlers(newTab);
                    }
                }
            }
        });

        /**
         * Gắn (Attach) handler (trình xử lý) cho các tab đã tồn tại (ví dụ: "Welcome")
         */
        for (Tab tab : mainTabPane.getTabs()) {
            attachDragHandlers(tab);
        }
    }

    /**
     * Gắn (Attach) các trình xử lý (handler)
     * kéo (drag) vào một Tab.
     *
     * @param tab Tab cần xử lý
     */
    private void attachDragHandlers(Tab tab) {
        /**
         * [SỬA LỖI] Kiểm tra 'graphic' TRƯỚC
         * vì các tab được gắn lại (re-dock) có text là null
         * nhưng graphic không null.
         */
        if (tab.getGraphic() != null) {
            return;
        }

        /**
         * Nếu graphic là null,
         * chúng ta mới kiểm tra text (ví dụ: cho tab "Welcome").
         */
        String tabText = tab.getText(); // Lấy text một lần
        if (tabText != null && tabText.equals("Welcome")) {
            return;
        }

        Label tabLabel = new Label(tabText); // Dùng text đã lấy

        /**
         * [SỬA LỖI] Sử dụng HBox làm graphic
         * để tăng vùng có thể click/kéo.
         */
        HBox tabGraphicContainer = new HBox(tabLabel);
        tabGraphicContainer.setStyle("-fx-padding: 4px 8px 4px 8px; -fx-alignment: CENTER;");
        tab.setGraphic(tabGraphicContainer);

        /**
         * [SỬA LỖI] Đặt text của tab thành null
         * để tránh hiển thị tên bị trùng lặp.
         */
        tab.setText(null);

        /**
         * Bắt đầu Kéo (Drag)
         */
        tabGraphicContainer.setOnDragDetected(event -> {
            String currentTabText = tabLabel.getText(); // Đọc text từ Label
            if (currentTabText == null || currentTabText.equals("Welcome")) {
                event.consume();
                return;
            }

            Dragboard db = tabGraphicContainer.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(currentTabText); // Sử dụng text từ Label
            db.setContent(content);

            draggingTab = tab; /** Đánh dấu (flag) tab đang kéo (drag) */

            db.setDragView(tabGraphicContainer.snapshot(null, null));
            event.consume();
        });

        /**
         * Xử lý Thả (Drop)
         */
        tabGraphicContainer.setOnDragDone(event -> {
            if (draggingTab == null) {
                return;
            }

            /**
             * Chụp (Capture) tab vào một biến (variable) final
             * TRƯỚC KHI set 'draggingTab' thành null
             * để tránh lỗi Race Condition / NullPointerException.
             */
            final Tab tabToUndock = draggingTab;
            draggingTab = null; /** Đặt (Set) lại cờ (flag) */

            Bounds tabPaneBounds = mainTabPane.localToScreen(mainTabPane.getBoundsInLocal());

            double dropX = event.getScreenX();
            double dropY = event.getScreenY();

            /**
             * Logic "Undock" (Tháo)
             */
            if (tabPaneBounds == null || !tabPaneBounds.contains(dropX, dropY)) {
                String text = tabLabel.getText(); // Sử dụng text từ Label
                logger.info("Đã phát hiện Thả (Drop) Tab '{}' ra ngoài. Đang undocking...", text);

                Platform.runLater(() -> {
                    if (mainTabPane.getTabs().contains(tabToUndock)) {
                        mainTabPane.getTabs().remove(tabToUndock);
                        /**
                         * Sử dụng biến (variable) đã chụp (captured)
                         */
                        viewManager.openNewWindowForTab(tabToUndock, mainTabPane);
                    }
                });
            }

            event.consume();
        });
    }

    /**
     * [CẬP NHẬT] Thiết lập logic UI cho bảng Backlinks (Cột phải).
     */
    private void setupBacklinksPanel() {
        backlinksListView.setItems(viewModel.getCurrentBacklinks());
        rightAccordion.setExpandedPane(backlinksPane);

        // [MỚI] Thêm CellFactory để render Artifact
        backlinksListView.setCellFactory(lv -> new ListCell<Artifact>() {
            @Override
            protected void updateItem(Artifact artifact, boolean empty) {
                super.updateItem(artifact, empty);
                if (empty || artifact == null) {
                    setText(null);
                } else {
                    setText(artifact.getId() + ": " + artifact.getName());
                }
            }
        });

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            viewModel.updateBacklinks(newTab);
        });
        viewModel.updateBacklinks(mainTabPane.getSelectionModel().getSelectedItem());

        // [SỬA LỖI] Cập nhật logic click
        backlinksListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Artifact selected = backlinksListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // Xây dựng (build) đường dẫn (path) chính xác
                    String relativePath = selected.getArtifactType() + File.separator + selected.getId() + ".json";
                    logger.info("Đang mở artifact (từ backlink): " + relativePath);
                    viewModel.openArtifact(relativePath);
                }
            }
        });
    }

    /**
     * [CẬP NHẬT] Thiết lập trình lắng nghe sự kiện
     * click chuột cho TreeView (Cột trái).
     * Giờ đây nó sẽ tìm ArtifactTreeItem.
     */
    private void setupTreeViewClickListener() {
        projectTreeView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();

                // [SỬA LỖI] Chỉ kiểm tra isLeaf,
                // và gọi getSelectedArtifactPath
                if (selectedItem != null && selectedItem.isLeaf()) {
                    String relativePath = getSelectedArtifactPath(selectedItem);
                    if (relativePath != null) {
                        logger.info("Đang mở artifact: " + relativePath);
                        viewModel.openArtifact(relativePath); // Đây là dòng 283 (trong log của bạn)
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
     * [CẬP NHẬT] Xử lý sự kiện khi người dùng chọn "Delete".
     * Sửa lại logic để hoạt động với ArtifactTreeItem.
     */
    @FXML
    private void handleDeleteArtifact() {
        TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();

        // [CẬP NHẬT] Lấy (get) path
        // (sẽ là null nếu nó là thư mục)
        String relativePath = getSelectedArtifactPath(selectedItem);

        if (relativePath == null) {
            showErrorAlert("Lỗi Xóa", "Vui lòng chọn một artifact (ví dụ: 'Đăng nhập người dùng') để xóa.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận Xóa");
        confirmAlert.setHeaderText("Bạn có chắc muốn xóa " + selectedItem.getValue() + "?");
        confirmAlert.setContentText("Hành động này không thể hoàn tác. Đường dẫn: " + relativePath);

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
     * [CẬP NHẬT] Hàm helper lấy đường dẫn tương đối (relative path)
     * từ TreeItem được chọn.
     *
     * @param selectedItem Mục (item) được chọn trong TreeView
     * @return Đường dẫn tương đối (ví dụ: "UC/UC001.json") hoặc null
     */
    private String getSelectedArtifactPath(TreeItem<String> selectedItem) {
        if (selectedItem instanceof ProjectServiceImpl.ArtifactTreeItem) {
            return ((ProjectServiceImpl.ArtifactTreeItem) selectedItem).getRelativePath();
        }
        return null; // Đây là một thư mục (folder) hoặc root
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
     * Xử lý sự kiện nhấn "View > Project Graph View".
     */
    @FXML
    private void handleOpenGraphView() {
        viewModel.openGraphViewTab();
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
     * Xử lý sự kiện nhấn "File > Import from Excel..."
     * Tuân thủ UC-PM-03 (Ngày 34).
     */
    @FXML
    private void handleImportExcel() {
        viewModel.openImportWizardTab();
    }

    /**
     * Xử lý sự kiện nhấn nút "Rebuild Index".
     * Tuân thủ UC-PM-04 (Ngày 37).
     */
    @FXML
    private void handleRebuildIndex() {
        viewModel.rebuildIndex();
    }

    /**
     * Xử lý sự kiện nhấn "Settings > API Keys..."
     * Tuân thủ UC-CFG-04 (Ngày 35).
     */
    @FXML
    private void handleOpenApiKeys() {
        viewModel.openApiKeysDialog();
    }

    /**
     * Hàm helper lấy Stage (cửa sổ) chính của ứng dụng.
     * @return Stage chính
     */
    private Stage getStage() {
        return (Stage) statusLabel.getScene().getWindow();
    }
}