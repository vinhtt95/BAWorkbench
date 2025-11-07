package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.viewmodel.FormBuilderViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * [CẬP NHẬT NGÀY 27 - SỬA LỖI GĐ 2]
 * "Dumb" View Controller
 * Đã nâng cấp để hỗ trợ Sửa (Edit) và Cột Properties.
 */
public class FormBuilderView {

    // --- FXML (CẬP NHẬT) ---
    @FXML private ListView<String> templateListView;
    @FXML private BorderPane editorPane;
    @FXML private TextField templateNameField;
    @FXML private TextField prefixIdField;
    @FXML private ListView<String> toolboxListView;
    @FXML private VBox formPreviewBox;
    @FXML private GridPane propertiesPane;

    private final FormBuilderViewModel viewModel;

    @Inject
    public FormBuilderView(FormBuilderViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        // --- 1. Binding (Liên kết) Cột Trái (Danh sách Template) ---
        templateListView.setItems(viewModel.templateNames);
        viewModel.loadTemplateNames(); // Tải dữ liệu

        // Khi click vào danh sách template
        templateListView.getSelectionModel().selectedItemProperty().addListener((obs, oldName, newName) -> {
            if (newName != null) {
                viewModel.loadTemplateForEditing(newName);
            }
        });

        // Khi ViewModel tải template xong, hiển thị/ẩn trình chỉnh sửa
        viewModel.currentTemplateProperty().addListener((obs, oldT, newT) -> {
            editorPane.setVisible(newT != null);
        });

        // --- 2. Binding (Liên kết) Trình Chỉnh sửa (Editor) ---
        templateNameField.textProperty().bindBidirectional(viewModel.templateName);
        prefixIdField.textProperty().bindBidirectional(viewModel.prefixId);
        toolboxListView.setItems(viewModel.toolboxItems);

        // --- 3. Logic Kéo-thả (Ngày 8) (Không đổi) ---
        setupDragAndDrop();

        // Cập nhật UI (Form Preview) khi ViewModel thay đổi
        viewModel.currentFields.addListener((ListChangeListener<ArtifactTemplate.FieldTemplate>) c -> {
            renderPreview();
        });

        // --- 4. Logic Cột Properties (MỚI) ---
        // Khi ViewModel chọn một field, cập nhật Cột Properties
        viewModel.selectedFieldProperty().addListener((obs, oldField, newField) -> {
            updatePropertiesPane(newField);
        });

        renderPreview(); // Render lần đầu (trống)
        updatePropertiesPane(null); // Render Cột Properties (trống)
    }

    /**
     * [MỚI] Hiển thị các trường (field) trong Cột Properties
     * (Tuân thủ UC-CFG-01 Bước 7.0)
     */
    private void updatePropertiesPane(ArtifactTemplate.FieldTemplate field) {
        propertiesPane.getChildren().clear();

        if (field != null) {
            // --- Tên (Name) ---
            propertiesPane.add(new Label("Name:"), 0, 0);
            TextField nameField = new TextField();
            nameField.textProperty().bindBidirectional(viewModel.currentFieldName);
            propertiesPane.add(nameField, 1, 0);

            // --- Loại (Type) ---
            propertiesPane.add(new Label("Type:"), 0, 1);
            TextField typeField = new TextField();
            typeField.textProperty().bind(viewModel.currentFieldType);
            typeField.setEditable(false); // Không cho sửa Type
            propertiesPane.add(typeField, 1, 1);

            // TODO: (Ngày 27+) Thêm cấu hình "Options" (ví dụ: nguồn Dropdown) ở đây
        }
    }

    /**
     * Render lại các control trong VBox
     * [CẬP NHẬT] Thêm listener click chuột vào các field
     */
    private void renderPreview() {
        formPreviewBox.getChildren().clear();
        if (viewModel.currentFields.isEmpty()) {
            formPreviewBox.getChildren().add(new Label("Kéo các trường từ Toolbox thả vào đây"));
        }

        for (ArtifactTemplate.FieldTemplate field : viewModel.currentFields) {
            Label fieldLabel = new Label(field.getName() + " (" + field.getType() + ")");
            fieldLabel.setStyle("-fx-border-color: #555555; -fx-padding: 10px; -fx-max-width: Infinity;");

            // [MỚI] Khi click vào field, kích hoạt Cột Properties
            fieldLabel.setOnMouseClicked(event -> {
                viewModel.selectField(field);
            });

            // [MỚI] Thêm style khi được chọn
            viewModel.selectedFieldProperty().addListener((obs, old, selected) -> {
                if (field == selected) {
                    fieldLabel.setStyle("-fx-border-color: -fx-accent; -fx-border-width: 2px; -fx-padding: 10px; -fx-max-width: Infinity;");
                } else {
                    fieldLabel.setStyle("-fx-border-color: #555555; -fx-padding: 10px; -fx-max-width: Infinity;");
                }
            });

            formPreviewBox.getChildren().add(fieldLabel);
        }
    }

    @FXML
    private void handleSaveTemplate() {
        // 14.0. BA nhấn "Save"
        viewModel.saveTemplate();
    }

    @FXML
    private void handleNewTemplate() {
        viewModel.createNewTemplate();
    }

    /**
     * (Không thay đổi)
     * Thiết lập Kéo (Drag) từ Toolbox và Thả (Drop) vào Form Preview
     */
    private void setupDragAndDrop() {
        // Thiết lập Kéo (Drag) từ Toolbox
        toolboxListView.setOnDragDetected(event -> {
            String selectedItem = toolboxListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                Dragboard db = toolboxListView.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(selectedItem);
                db.setContent(content);
                event.consume();
            }
        });

        // Thiết lập Thả (Drop) vào Form Preview
        formPreviewBox.setOnDragOver(event -> {
            if (event.getGestureSource() != formPreviewBox && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        // Xử lý khi thả
        formPreviewBox.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String fieldType = db.getString();

                // Thêm vào ViewModel
                ArtifactTemplate.FieldTemplate newField = new ArtifactTemplate.FieldTemplate();
                newField.setName(fieldType); // Tên tạm thời
                newField.setType(fieldType);
                viewModel.currentFields.add(newField);

                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
}