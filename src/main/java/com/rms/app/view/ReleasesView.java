package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.viewmodel.ReleasesViewModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDate;

/**
 * "Dumb" View Controller cho ReleasesView.fxml (UC-CFG-02).
 * Chịu trách nhiệm binding dữ liệu từ ViewModel và gọi các hàm
 * trên ViewModel khi có sự kiện.
 */
public class ReleasesView {

    @FXML private TableView<ReleasesViewModel.ReleaseModel> releasesTableView;
    @FXML private TableColumn<ReleasesViewModel.ReleaseModel, String> releaseIdColumn;
    @FXML private TableColumn<ReleasesViewModel.ReleaseModel, String> releaseNameColumn;
    @FXML private TableColumn<ReleasesViewModel.ReleaseModel, LocalDate> releaseDateColumn;

    @FXML private TextField idField;
    @FXML private TextField nameField;
    @FXML private DatePicker dateField;

    @FXML private Button newButton;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;

    private final ReleasesViewModel viewModel;

    /**
     * Binding 2 chiều thủ công cho Form (Cột phải).
     */
    private final ObjectProperty<ReleasesViewModel.ReleaseModel> formModel = new SimpleObjectProperty<>();

    @Inject
    public ReleasesView(ReleasesViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        /**
         * 1. Thiết lập Bảng (TableView)
         */
        releaseIdColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        releaseNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        releaseDateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());

        releasesTableView.setItems(viewModel.getReleasesList());

        /**
         * 2. Lắng nghe Bảng (TableView) -> Cập nhật ViewModel
         */
        releasesTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            viewModel.selectedReleaseProperty().set(newSelection);
            deleteButton.setDisable(newSelection == null);
            saveButton.setDisable(newSelection == null);
        });

        /**
         * 3. Lắng nghe ViewModel -> Cập nhật Form
         */
        viewModel.selectedReleaseProperty().addListener((obs, oldRelease, newRelease) -> {
            if (newRelease != null) {
                bindFormToModel(newRelease);
            } else {
                clearForm();
            }
        });

        /**
         * 4. Khởi tạo Form (Trống)
         */
        bindFormToModel(null);

        /**
         * 5. Tải dữ liệu
         */
        viewModel.loadReleases();
    }

    /**
     * Liên kết các trường (field) trong Form (Cột phải) với một Model (View).
     *
     * @param model Model (View) để bind, hoặc null để xóa bind.
     */
    private void bindFormToModel(ReleasesViewModel.ReleaseModel model) {
        /**
         * Hủy binding cũ
         */
        if (formModel.get() != null) {
            idField.textProperty().unbindBidirectional(formModel.get().idProperty());
            nameField.textProperty().unbindBidirectional(formModel.get().nameProperty());
            dateField.valueProperty().unbindBidirectional(formModel.get().dateProperty());
        }

        formModel.set(model);

        /**
         * Bind mới (nếu model tồn tại)
         */
        if (model != null) {
            idField.textProperty().bindBidirectional(model.idProperty());
            nameField.textProperty().bindBidirectional(model.nameProperty());
            dateField.valueProperty().bindBidirectional(model.dateProperty());
            idField.setEditable(false);
        } else {
            clearForm();
        }
    }

    /**
     * Xóa sạch Form (khi nhấn "New" hoặc bỏ chọn).
     */
    private void clearForm() {
        idField.clear();
        nameField.clear();
        dateField.setValue(null);
        idField.setEditable(true);
    }

    /**
     * Xử lý sự kiện nhấn nút "Tạo mới".
     */
    @FXML
    private void handleNew() {
        releasesTableView.getSelectionModel().clearSelection();
        ReleasesViewModel.ReleaseModel newModel = new ReleasesViewModel.ReleaseModel("", "", LocalDate.now());
        bindFormToModel(newModel);
        idField.requestFocus();
        saveButton.setDisable(false);
    }

    /**
     * Xử lý sự kiện nhấn nút "Lưu thay đổi".
     */
    @FXML
    private void handleSave() {
        ReleasesViewModel.ReleaseModel modelToSave = formModel.get();
        if (modelToSave == null) {
            /**
             * Trường hợp này không nên xảy ra nếu nút "Save" được enable.
             */
            showErrorAlert("Lỗi Lưu", "Không có Release nào được chọn để lưu.");
            return;
        }

        try {
            /**
             * ViewModel xử lý logic nghiệp vụ (Kiểm tra trùng lặp)
             */
            viewModel.saveOrUpdateRelease(modelToSave);
            releasesTableView.getSelectionModel().select(modelToSave);
        } catch (IOException e) {
            showErrorAlert("Lỗi Lưu (UC-CFG-02, 1.0.E1)", e.getMessage());
        }
    }

    /**
     * Xử lý sự kiện nhấn nút "Xóa".
     */
    @FXML
    private void handleDelete() {
        ReleasesViewModel.ReleaseModel modelToDelete = viewModel.selectedReleaseProperty().get();
        if (modelToDelete == null) {
            showErrorAlert("Lỗi Xóa", "Vui lòng chọn một Release từ danh sách để xóa.");
            return;
        }

        try {
            /**
             * ViewModel xử lý logic nghiệp vụ (Kiểm tra toàn vẹn)
             */
            viewModel.deleteRelease(modelToDelete);
        } catch (IOException e) {
            showErrorAlert("Lỗi Xóa (UC-CFG-02, 1.0.A2)", e.getMessage());
        }
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
}