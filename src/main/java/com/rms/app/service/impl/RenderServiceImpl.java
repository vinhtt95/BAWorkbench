package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.FlowStep;
import com.rms.app.service.IRenderService;
import com.rms.app.service.ISearchService;
import com.rms.app.view.FlowBuilderControl;
import com.rms.app.viewmodel.ArtifactViewModel;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai logic render Form động (UC-DEV-01)
 * và logic Autocomplete (UC-DEV-02).
 */
public class RenderServiceImpl implements IRenderService {

    private static final Logger logger = LoggerFactory.getLogger(RenderServiceImpl.class);
    private final Injector injector;
    private final ISearchService searchService;
    private final ContextMenu autocompletePopup;

    @Inject
    public RenderServiceImpl(Injector injector) {
        this.injector = injector;
        this.searchService = injector.getInstance(ISearchService.class);
        this.autocompletePopup = new ContextMenu();
    }

    @Override
    public List<Node> renderForm(ArtifactTemplate template, ArtifactViewModel viewModel) {
        List<Node> nodes = new ArrayList<>();
        if (template == null || template.getFields() == null) {
            return nodes;
        }

        nodes.add(createFieldGroup("ID", new TextField(viewModel.getId()) {{ setEditable(false); }}));
        nodes.add(createFieldGroup("Name", new TextField() {{
            textProperty().bindBidirectional(viewModel.nameProperty());
        }}));

        for (ArtifactTemplate.FieldTemplate field : template.getFields()) {
            Node control = createControlForField(field, viewModel);
            if (control != null) {
                nodes.add(createFieldGroup(field.getName(), control));
            }
        }
        return nodes;
    }

    /**
     * Tạo control JavaFX tương ứng cho một FieldTemplate.
     *
     * @param field     Template của trường (field)
     * @param viewModel ViewModel để bind dữ liệu
     * @return Một Node (control) JavaFX
     */
    private Node createControlForField(ArtifactTemplate.FieldTemplate field, ArtifactViewModel viewModel) {

        /**
         * [SỬA LỖI 2] Xử lý Flow Builder riêng biệt TRƯỚC KHI
         * gọi getFieldProperty để tránh xung đột map.
         */
        if ("Flow Builder".equals(field.getType())) {
            try {
                ObservableList<FlowStep> steps = viewModel.getFlowStepProperty(field.getName());
                return loadFlowBuilderControl(steps);
            } catch (IOException e) {
                logger.error("Không thể tải FlowBuilderControl.fxml", e);
                return new Label("Lỗi khi tải Flow Builder");
            }
        }

        /**
         * Logic cũ (hiện đã an toàn)
         */
        var fieldProperty = viewModel.getFieldProperty(field.getName());

        switch (field.getType()) {
            case "Text (Single-line)":
                TextField textField = new TextField();
                textField.textProperty().bindBidirectional((StringProperty) fieldProperty);
                return textField;

            case "Text Area (Multi-line)":
                TextArea textArea = new TextArea();
                textArea.setPrefRowCount(5);
                textArea.textProperty().bindBidirectional((StringProperty) fieldProperty);
                setupAutocomplete(textArea);
                return textArea;

            case "Dropdown":
                var stringPropDrop = (StringProperty) viewModel.getFieldProperty(field.getName());
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.getItems().addAll("Option 1", "Option 2");
                comboBox.valueProperty().bindBidirectional(stringPropDrop);
                return comboBox;

            case "Linker (@ID)":
                var stringPropLink = (StringProperty) viewModel.getFieldProperty(field.getName());
                TextField linkerField = new TextField();
                linkerField.textProperty().bindBidirectional((StringProperty) stringPropLink);
                linkerField.setPromptText("Gõ @ để tìm kiếm...");
                setupAutocomplete(linkerField);
                return linkerField;

            default:
                logger.warn("Loại field không xác định: {}", field.getType());
                return null;
        }
    }

    /**
     * Thêm logic Autocomplete (gợi ý @ID) vào một control (TextField hoặc TextArea).
     *
     * @param control Control (TextField, TextArea) cần thêm tính năng.
     */
    private void setupAutocomplete(final TextInputControl control) {
        control.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            String text = control.getText();
            if (text.isEmpty()) {
                autocompletePopup.hide();
                return;
            }

            int atPos = text.lastIndexOf('@', newPos.intValue() - 1);
            if (atPos == -1) {
                autocompletePopup.hide();
                return;
            }

            String queryPart = text.substring(atPos + 1, newPos.intValue());
            if (queryPart.contains(" ") || queryPart.contains("\n")) {
                autocompletePopup.hide();
                return;
            }

            List<Artifact> results = searchService.search(queryPart);
            if (results.isEmpty()) {
                autocompletePopup.hide();
                return;
            }

            autocompletePopup.getItems().clear();
            for (Artifact artifact : results) {
                String itemText = String.format("%s: %s", artifact.getId(), artifact.getName());
                MenuItem item = new MenuItem(itemText);
                item.setOnAction(e -> {
                    String newText = text.substring(0, atPos) + "@" + artifact.getId() + " " + text.substring(newPos.intValue());
                    control.setText(newText);
                    control.positionCaret(atPos + artifact.getId().length() + 2);
                    autocompletePopup.hide();
                });
                autocompletePopup.getItems().add(item);
            }
            autocompletePopup.show(control, Side.BOTTOM, 0, 0);
        });

        control.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                autocompletePopup.hide();
            }
        });
    }

    /**
     * Tải FXML control FlowBuilderControl và inject data (list) vào.
     *
     * @param steps Danh sách các bước quy trình
     * @return Node (control) FlowBuilder
     * @throws IOException Nếu không thể tải FXML
     */
    private Node loadFlowBuilderControl(ObservableList<FlowStep> steps) throws IOException {
        String fxmlPath = "/com/rms/app/view/FlowBuilderControl.fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

        loader.setControllerFactory(injector::getInstance);
        Parent controlRoot = loader.load();

        FlowBuilderControl controller = loader.getController();
        controller.setData(steps);

        return controlRoot;
    }

    /**
     * Helper tạo nhóm Label + Control cho Form.
     *
     * @param label   Nhãn của trường
     * @param control Control (TextField, v.v.)
     * @return Một VBox chứa Label và Control
     */
    private Node createFieldGroup(String label, Node control) {
        VBox fieldGroup = new VBox(5);
        Label fieldLabel = new Label(label + ":");
        fieldLabel.setStyle("-fx-font-weight: bold;");
        fieldGroup.getChildren().addAll(fieldLabel, control);
        VBox.setMargin(fieldGroup, new Insets(10, 0, 0, 0));
        return fieldGroup;
    }
}