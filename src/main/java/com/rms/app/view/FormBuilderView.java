package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.viewmodel.FormBuilderViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

// "Dumb" View Controller
public class FormBuilderView {

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
        // --- 1. Binding (Liên kết) ---
        templateNameField.textProperty().bindBidirectional(viewModel.templateName);
        prefixIdField.textProperty().bindBidirectional(viewModel.prefixId);
        toolboxListView.setItems(viewModel.toolboxItems);

        // --- 2. Logic Kéo-thả (Ngày 8) ---

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

        // Cập nhật UI (Form Preview) khi ViewModel thay đổi
        viewModel.currentFields.addListener((ListChangeListener<ArtifactTemplate.FieldTemplate>) c -> {
            renderPreview();
        });

        renderPreview(); // Render lần đầu
    }

    // Render lại các control trong VBox
    private void renderPreview() {
        formPreviewBox.getChildren().clear();
        if (viewModel.currentFields.isEmpty()) {
            formPreviewBox.getChildren().add(new Label("Kéo các trường từ Toolbox thả vào đây"));
        }

        for (ArtifactTemplate.FieldTemplate field : viewModel.currentFields) {
            Label fieldLabel = new Label(field.getName() + " (" + field.getType() + ")");
            fieldLabel.setStyle("-fx-border-color: #555555; -fx-padding: 10px; -fx-max-width: Infinity;");
            formPreviewBox.getChildren().add(fieldLabel);
        }
    }

    @FXML
    private void handleSaveTemplate() {
        // 14.0. BA nhấn "Save"
        viewModel.saveTemplate();
    }
}