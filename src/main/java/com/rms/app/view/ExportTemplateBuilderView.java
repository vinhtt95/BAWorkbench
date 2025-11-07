package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.ExportTemplateSection;
import com.rms.app.viewmodel.ExportTemplateBuilderViewModel;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;

/**
 * "Dumb" View Controller cho ExportTemplateBuilderView.fxml (UC-CFG-03).
 */
public class ExportTemplateBuilderView {

    @FXML private ListView<String> templateListView;
    @FXML private BorderPane editorPane;
    @FXML private TextField templateNameField;
    @FXML private ListView<ExportTemplateSection> sectionsListView;
    @FXML private VBox propertiesPane;

    @FXML private TextField titleField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private VBox staticPane;
    @FXML private TextArea contentField;
    @FXML private VBox dynamicPane;
    @FXML private ComboBox<String> queryTypeComboBox;
    @FXML private ComboBox<String> queryStatusComboBox;
    @FXML private ComboBox<String> displayFormatComboBox;

    private final ExportTemplateBuilderViewModel viewModel;

    @Inject
    public ExportTemplateBuilderView(ExportTemplateBuilderViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        /**
         * Cột 1: Danh sách Template
         */
        templateListView.setItems(viewModel.templateNames);
        viewModel.loadTemplateNames();

        templateListView.getSelectionModel().selectedItemProperty().addListener((obs, oldName, newName) -> {
            viewModel.loadTemplateForEditing(newName);
        });

        /**
         * Cột 2: Trình chỉnh sửa (Editor)
         */
        editorPane.visibleProperty().bind(Bindings.isNotNull(viewModel.templateName));
        templateNameField.textProperty().bindBidirectional(viewModel.templateName);

        /**
         * Cột 2: Danh sách Chương (Section)
         */
        sectionsListView.setItems(viewModel.currentSections);
        sectionsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ExportTemplateSection item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + " (" + item.getType() + ")");
                }
            }
        });

        sectionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSection, newSection) -> {
            viewModel.selectedSection.set(newSection);
        });

        /**
         * Lắng nghe Cột 2 (Danh sách Section) để refresh (Hack)
         */
        viewModel.currentSections.addListener((ListChangeListener<ExportTemplateSection>) c -> sectionsListView.refresh());

        /**
         * Cột 3: Thuộc tính (Properties)
         */
        propertiesPane.visibleProperty().bind(viewModel.selectedSection.isNotNull());

        /**
         * [SỬA LỖI] Xóa bỏ (remove) phương thức bindPropertiesPane()
         * và thay thế bằng binding hai chiều (bidirectional) trực tiếp
         * tới các Property mới trong ViewModel.
         */
        titleField.textProperty().bindBidirectional(viewModel.currentSectionTitle);
        typeComboBox.valueProperty().bindBidirectional(viewModel.currentSectionType);
        contentField.textProperty().bindBidirectional(viewModel.currentSectionContent);
        queryTypeComboBox.valueProperty().bindBidirectional(viewModel.currentQueryType);
        queryStatusComboBox.valueProperty().bindBidirectional(viewModel.currentQueryStatus);
        displayFormatComboBox.valueProperty().bindBidirectional(viewModel.currentDisplayFormat);

        /**
         * Thiết lập các ComboBox
         */
        typeComboBox.setItems(viewModel.sectionTypeOptions);
        displayFormatComboBox.setItems(viewModel.displayFormatOptions);
        queryTypeComboBox.setItems(viewModel.artifactTypeOptions);
        queryStatusComboBox.setItems(viewModel.statusOptions);

        /**
         * [SỬA LỖI] Logic ẩn/hiện (show/hide)
         * các panel Tĩnh/Động (Static/Dynamic)
         * dựa trên Property của ViewModel.
         */
        viewModel.currentSectionType.addListener((obs, oldV, newV) -> {
            updatePanelVisibility(newV);
        });
        updatePanelVisibility(null); // Ẩn cả hai khi bắt đầu
    }

    /**
     * Ẩn/Hiện panel Tĩnh (Static) hoặc Động (Dynamic)
     *
     * @param type Loại (Type)
     */
    private void updatePanelVisibility(String type) {
        staticPane.setVisible("Static".equals(type));
        staticPane.setManaged("Static".equals(type));
        dynamicPane.setVisible("Dynamic".equals(type));
        dynamicPane.setManaged("Dynamic".equals(type));
    }


    @FXML
    private void handleNewTemplate() {
        viewModel.createNewTemplate();
    }

    @FXML
    private void handleSaveTemplate() {
        viewModel.saveTemplate();
    }

    @FXML
    private void handleAddSection() {
        viewModel.addSection();
    }

    @FXML
    private void handleRemoveSection() {
        viewModel.removeSection();
    }
}