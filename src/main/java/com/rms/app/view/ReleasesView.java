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
        });

        /**
         * 3. Lắng nghe ViewModel -> Cập nhật Form
         */
        viewModel.selectedReleaseProperty().addListener((obs, oldRelease, newRelease) -> {
            if (newRelease != null) {
                bindFormToModel(newRelease);
            } else {
                /**
                 * [SỬA LỖI] Nếu bỏ chọn (hoặc sau khi Tạo mới),
                 * form sẽ được clear và disable.
                 */
                bindFormToModel(null);
            }
        });

        /**
         * 4. Khởi tạo Form (Trống)
         * [SỬA LỖI] Bước này sẽ vô hiệu hóa (disable) Form khi bắt đầu
         */
        bindFormToModel(null);

        /**
         * 5. Tải dữ liệu
         */
        viewModel.loadReleases();
    }

    /**
     * [SỬA LỖI] Helper (hàm phụ) để vô hiệu hóa (disable)
     * hoặc kích hoạt (enable) toàn bộ Form chi tiết.
     *
     * @param disabled true = vô hiệu hóa, false = kích hoạt
     */
    private void setFormDisabled(boolean disabled) {
        idField.setDisable(disabled);
        nameField.setDisable(disabled);
        dateField.setDisable(disabled);
        saveButton.setDisable(disabled);
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
            setFormDisabled(false); // [SỬA LỖI] Kích hoạt (enable) form
        } else {
            clearForm();
            setFormDisabled(true); // [SỬA LỖI] Vô hiệu hóa (disable) form
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

        /**
         * [SỬA LỖI] Ghi đè (Override)
         * bindFormToModel (vì nó set editable=false)
         * để cho phép nhập ID mới.
         */
        idField.setEditable(true);
        idField.requestFocus();
    }

    /**
     * Xử lý sự kiện nhấn nút "Lưu thay đổi".
     */
    @FXML
    private void handleSave() {
        ReleasesViewModel.ReleaseModel modelToSave = formModel.get();
        if (modelToSave == null) {
            showErrorAlert("Lỗi Lưu", "Không có Release nào được chọn để lưu.");
            return;
        }

        try {
            /**
             * [SỬA LỖI] Yêu cầu ViewModel
             * trả về đối tượng đã được lưu (và tải lại)
             */
            ReleasesViewModel.ReleaseModel savedModel = viewModel.saveOrUpdateRelease(modelToSave);

            /**
             * [SỬA LỖI] Chọn đối tượng 'savedModel' (đối tượng mới
             * từ danh sách đã tải lại) thay vì 'modelToSave' (đối tượng cũ).
             * Điều này ngăn 'TableView' mất lựa chọn
             * và ngăn 'Form' bị xóa trắng.
             */
            if (savedModel != null) {
                releasesTableView.getSelectionModel().select(savedModel);
            } else {
                /**
                 * Trường hợp này xảy ra nếu 'save' thất bại và trả về null
                 * (ví dụ: do ID trùng lặp đã được xử lý trong ViewModel)
                 */
                releasesTableView.getSelectionModel().clearSelection();
            }
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