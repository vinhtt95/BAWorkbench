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

    /**
     * Flag (cờ) để ngăn chặn các vòng lặp (loop) binding
     */
    private boolean isBinding = false;

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
         *
         * [SỬA LỖI NGÀY 32]
         * Thay đổi .templateNameProperty() thành .templateName
         * để khớp với thuộc tính (field) public trong ViewModel.
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
         * Cột 3: Thuộc tính (Properties)
         */
        propertiesPane.visibleProperty().bind(viewModel.selectedSection.isNotNull());
        viewModel.selectedSection.addListener((obs, oldSection, newSection) -> {
            bindPropertiesPane(newSection);
        });

        /**
         * Thiết lập các ComboBox
         */
        typeComboBox.setItems(viewModel.sectionTypeOptions);
        displayFormatComboBox.setItems(viewModel.displayFormatOptions);
        queryTypeComboBox.setItems(viewModel.artifactTypeOptions);
        queryStatusComboBox.setItems(viewModel.statusOptions);

        /**
         * Lắng nghe Cột 2 (Danh sách Section) để refresh (Hack)
         */
        viewModel.currentSections.addListener((ListChangeListener<ExportTemplateSection>) c -> sectionsListView.refresh());
    }

    /**
     * Binding (thủ công) Cột 3 (Properties) với Section được chọn.
     *
     * @param section Section (chương) được chọn
     */
    private void bindPropertiesPane(ExportTemplateSection section) {
        isBinding = true;

        if (section == null) {
            titleField.setText("");
            typeComboBox.setValue(null);
            contentField.setText("");
            queryTypeComboBox.setValue(null);
            queryStatusComboBox.setValue(null);
            displayFormatComboBox.setValue(null);
            staticPane.setVisible(false);
            dynamicPane.setVisible(false);
            isBinding = false;
            return;
        }

        /**
         * Hủy binding cũ (nếu có)
         */
        titleField.textProperty().unbind();
        typeComboBox.valueProperty().unbind();
        contentField.textProperty().unbind();
        queryTypeComboBox.valueProperty().unbind();
        queryStatusComboBox.valueProperty().unbind();
        displayFormatComboBox.valueProperty().unbind();

        /**
         * 1. Bind dữ liệu TỪ Model (POJO) -> View (UI)
         */
        titleField.setText(section.getTitle());
        typeComboBox.setValue(section.getType());
        contentField.setText(section.getContent());
        displayFormatComboBox.setValue(section.getDisplayFormat());

        if (section.getQuery() != null) {
            queryTypeComboBox.setValue(section.getQuery().get("artifactType"));
            queryStatusComboBox.setValue(section.getQuery().get("status"));
        } else {
            section.setQuery(new HashMap<>());
            queryTypeComboBox.setValue(null);
            queryStatusComboBox.setValue(null);
        }

        /**
         * 2. Hiển thị panel (Tĩnh/Động) chính xác
         */
        updatePanelVisibility(section.getType());

        /**
         * 3. Bind dữ liệu TỪ View (UI) -> Model (POJO)
         */
        titleField.textProperty().addListener((obs, oldV, newV) -> {
            if (!isBinding) section.setTitle(newV);
            sectionsListView.refresh();
        });

        contentField.textProperty().addListener((obs, oldV, newV) -> {
            if (!isBinding) section.setContent(newV);
        });

        displayFormatComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isBinding) section.setDisplayFormat(newV);
        });

        queryTypeComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isBinding) section.getQuery().put("artifactType", newV);
        });

        queryStatusComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isBinding) section.getQuery().put("status", newV);
        });

        /**
         * 4. Xử lý logic khi thay đổi Loại (Type)
         */
        typeComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isBinding) {
                section.setType(newV);
                updatePanelVisibility(newV);
            }
        });

        isBinding = false;
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