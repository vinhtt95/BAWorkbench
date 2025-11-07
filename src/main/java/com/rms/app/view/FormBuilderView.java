package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.viewmodel.FormBuilderViewModel;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;

/**
 * [CẬP NHẬT]
 * "Dumb" View Controller
 * Đã nâng cấp để hỗ trợ Sắp xếp, Xóa, Thuộc tính (Options),
 * và dùng ListView cho Preview.
 * [SỬA LỖI] Sửa lỗi UX khi Up/Down (selection không di chuyển).
 */
public class FormBuilderView {

    // --- FXML (CẬP NHẬT) ---
    @FXML private ListView<String> templateListView;
    @FXML private BorderPane editorPane;
    @FXML private TextField templateNameField;
    @FXML private TextField prefixIdField;
    @FXML private Label versionLabel;
    @FXML private ListView<String> toolboxListView;

    // [CẬP NHẬT] Thay VBox bằng ListView
    @FXML private ListView<ArtifactTemplate.FieldTemplate> formPreviewListView;
    @FXML private Button moveUpButton;
    @FXML private Button moveDownButton;
    @FXML private Button removeFieldButton;

    // [CẬP NHẬT] Cột Properties
    @FXML private VBox propertiesPaneContainer;
    @FXML private GridPane propertiesPane;
    @FXML private Label noFieldSelectedLabel;
    @FXML private TextField fieldNameField;
    @FXML private TextField fieldTypeField;
    @FXML private VBox dropdownOptionsPane;
    @FXML private TextArea dropdownOptionsArea;


    private final FormBuilderViewModel viewModel;

    /**
     * DataFormat tùy chỉnh cho việc kéo-thả (drag-and-drop)
     * các trường (field) BÊN TRONG ListView (để sắp xếp lại)
     */
    private static final DataFormat FIELD_TEMPLATE_FORMAT =
            new DataFormat("com.rms.app.model.ArtifactTemplate.FieldTemplate");

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
            viewModel.loadTemplateForEditing(newName);
        });

        // Khi ViewModel tải template xong, hiển thị/ẩn trình chỉnh sửa
        viewModel.currentTemplateProperty().addListener((obs, oldT, newT) -> {
            editorPane.setVisible(newT != null);
        });

        // --- 2. Binding (Liên kết) Trình Chỉnh sửa (Editor) ---
        templateNameField.textProperty().bindBidirectional(viewModel.templateName);
        prefixIdField.textProperty().bindBidirectional(viewModel.prefixId);
        versionLabel.textProperty().bind(viewModel.currentVersion.asString());
        toolboxListView.setItems(viewModel.toolboxItems);

        // --- 3. Binding (Liên kết) Cột Giữa (Preview) ---
        formPreviewListView.setItems(viewModel.currentFields);
        // Tùy chỉnh cách ListView hiển thị
        formPreviewListView.setCellFactory(lv -> new FieldTemplateListCell());

        // Liên kết field đã chọn trong VM với ListView
        formPreviewListView.getSelectionModel().selectedItemProperty().addListener((obs, oldField, newField) -> {
            viewModel.selectField(newField);
        });
        // (Binding ngược lại)
        viewModel.selectedFieldProperty().addListener((obs, oldField, newField) -> {
            if (newField != formPreviewListView.getSelectionModel().getSelectedItem()) {
                formPreviewListView.getSelectionModel().select(newField);
            }
            formPreviewListView.refresh(); // Refresh để highlight
        });

        // Vô hiệu hóa (disable) các nút nếu không có gì được chọn
        moveUpButton.disableProperty().bind(viewModel.selectedFieldProperty().isNull());
        moveDownButton.disableProperty().bind(viewModel.selectedFieldProperty().isNull());
        removeFieldButton.disableProperty().bind(viewModel.selectedFieldProperty().isNull());


        // --- 4. Logic Kéo-thả (CẬP NHẬT) ---
        setupToolboxDrag();
        setupPreviewDrop(); // Thả (drop) TỪ Toolbox
        // (Kéo-thả BÊN TRONG ListView
        // được xử lý trong custom ListCell)

        // --- 5. Logic Cột Properties (CẬP NHẬT) ---
        bindPropertiesPane();
    }

    /**
     * [MỚI] Liên kết Cột Thuộc tính (Properties) (Cột 3)
     */
    private void bindPropertiesPane() {
        // Ẩn/Hiện toàn bộ panel
        propertiesPane.visibleProperty().bind(viewModel.selectedFieldProperty().isNotNull());
        propertiesPane.managedProperty().bind(viewModel.selectedFieldProperty().isNotNull());
        noFieldSelectedLabel.visibleProperty().bind(viewModel.selectedFieldProperty().isNull());
        noFieldSelectedLabel.managedProperty().bind(viewModel.selectedFieldProperty().isNull());

        // Bind các trường (field) chung
        fieldNameField.textProperty().bindBidirectional(viewModel.currentFieldName);
        fieldTypeField.textProperty().bind(viewModel.currentFieldType);

        // Bind các trường (field) tùy chọn (Dropdown)
        dropdownOptionsArea.textProperty().bindBidirectional(viewModel.currentFieldOptions);

        // Ẩn/Hiện panel tùy chọn (options)
        // (Chỉ lắng nghe kiểu (type) thay đổi)
        viewModel.currentFieldType.addListener((obs, oldType, newType) -> {
            boolean isDropdown = "Dropdown".equals(newType);
            dropdownOptionsPane.setVisible(isDropdown);
            dropdownOptionsPane.setManaged(isDropdown);
        });
        // Đặt (set) trạng thái ban đầu
        dropdownOptionsPane.setVisible(false);
        dropdownOptionsPane.setManaged(false);
    }

    @FXML
    private void handleSaveAsNewVersion() {
        viewModel.saveAsNewVersion();
    }

    @FXML
    private void handleNewTemplate() {
        viewModel.createNewTemplate();
    }

    @FXML
    private void handleRemoveField() {
        viewModel.removeSelectedField();
    }

    /**
     * [SỬA LỖI UX] Cập nhật lại selection
     * sau khi ViewModel di chuyển item.
     */
    @FXML
    private void handleMoveUp() {
        int selectedIndex = formPreviewListView.getSelectionModel().getSelectedIndex();
        viewModel.moveSelectedFieldUp(); // ViewModel chỉ hoán đổi (swap) list

        // [FIX] View tự cập nhật lại selection
        if (selectedIndex > 0) {
            formPreviewListView.getSelectionModel().select(selectedIndex - 1);
        }
    }

    /**
     * [SỬA LỖI UX] Cập nhật lại selection
     * sau khi ViewModel di chuyển item.
     */
    @FXML
    private void handleMoveDown() {
        int selectedIndex = formPreviewListView.getSelectionModel().getSelectedIndex();
        viewModel.moveSelectedFieldDown(); // ViewModel chỉ hoán đổi (swap) list

        // [FIX] View tự cập nhật lại selection
        if (selectedIndex < viewModel.currentFields.size() - 1) {
            formPreviewListView.getSelectionModel().select(selectedIndex + 1);
        }
    }

    /**
     * (CẬP NHẬT)
     * Thiết lập Kéo (Drag) từ Toolbox
     */
    private void setupToolboxDrag() {
        toolboxListView.setOnDragDetected(event -> {
            String selectedItem = toolboxListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                Dragboard db = toolboxListView.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(selectedItem); // Kéo (drag) tên của Loại (Type)
                db.setContent(content);
                event.consume();
            }
        });
    }

    /**
     * (CẬP NHẬT)
     * Thiết lập Thả (Drop) vào Form Preview (ListView)
     */
    private void setupPreviewDrop() {
        formPreviewListView.setOnDragOver(event -> {
            // Chỉ chấp nhận nếu kéo (drag) từ Toolbox
            // (Không phải là một FieldTemplate)
            if (event.getGestureSource() != formPreviewListView && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // Xử lý khi thả
        formPreviewListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String fieldType = db.getString();

                // Thêm vào ViewModel
                ArtifactTemplate.FieldTemplate newField = new ArtifactTemplate.FieldTemplate();
                newField.setName(fieldType); // Tên tạm thời
                newField.setType(fieldType);
                newField.setOptions(new HashMap<>()); // [MỚI] Khởi tạo map
                viewModel.currentFields.add(newField);

                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }


    /**
     * [MỚI] Lớp (class) tùy chỉnh
     * để render các trường (field) trong ListView Preview
     * và xử lý Kéo-Thả (Drag-and-Drop) SẮP XẾP LẠI (Re-order)
     */
    private class FieldTemplateListCell extends ListCell<ArtifactTemplate.FieldTemplate> {
        public FieldTemplateListCell() {
            setOnDragDetected(event -> {
                if (getItem() == null) {
                    return;
                }
                Dragboard db = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.put(FIELD_TEMPLATE_FORMAT, getItem());
                db.setContent(content);
                event.consume();
            });

            setOnDragOver(event -> {
                if (event.getGestureSource() != this && event.getDragboard().hasContent(FIELD_TEMPLATE_FORMAT)) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            setOnDragDropped(event -> {
                if (getItem() == null) {
                    return;
                }
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasContent(FIELD_TEMPLATE_FORMAT)) {
                    ArtifactTemplate.FieldTemplate draggedField =
                            (ArtifactTemplate.FieldTemplate) db.getContent(FIELD_TEMPLATE_FORMAT);
                    int draggedIndex = viewModel.currentFields.indexOf(draggedField);
                    int thisIndex = getIndex();

                    // Di chuyển
                    viewModel.currentFields.remove(draggedIndex);
                    viewModel.currentFields.add(thisIndex, draggedField);

                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });
        }

        @Override
        protected void updateItem(ArtifactTemplate.FieldTemplate field, boolean empty) {
            super.updateItem(field, empty);
            if (empty || field == null) {
                setText(null);
                setStyle("");
            } else {
                setText(field.getName() + " (" + field.getType() + ")");

                // [SỬA LỖI] Luôn đặt màu text
                // để tránh lỗi "UI mù mắt"
                String textFill = "-fx-text-fill: #E3E3E3;";

                // [MỚI] Highlight (làm nổi bật) nếu được chọn
                if (viewModel.selectedFieldProperty().get() == field) {
                    setStyle("-fx-border-color: -fx-accent; " +
                            "-fx-border-width: 1px; " +
                            "-fx-background-color: -fx-accent;" + // [SỬA LỖI] Dùng -fx-accent
                            textFill);
                } else {
                    setStyle("-fx-border-color: #555555; " +
                            "-fx-border-width: 0 0 1px 0; " +
                            "-fx-padding: 8px;" +
                            textFill);
                }
            }
        }
    }
}