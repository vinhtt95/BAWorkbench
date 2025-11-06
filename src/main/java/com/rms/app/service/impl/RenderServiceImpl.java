package com.rms.app.service.impl;

import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.IRenderService;
import com.rms.app.viewmodel.ArtifactViewModel;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Triển khai logic render Form
public class RenderServiceImpl implements IRenderService {

    private static final Logger logger = LoggerFactory.getLogger(RenderServiceImpl.class);

    @Override
    public List<Node> renderForm(ArtifactTemplate template, ArtifactViewModel viewModel) {
        List<Node> nodes = new ArrayList<>();
        if (template == null || template.getFields() == null) {
            return nodes;
        }

        // Render các trường (fields) cơ bản trước
        nodes.add(createFieldGroup("ID", new TextField(viewModel.getId()) {{ setEditable(false); }}));
        nodes.add(createFieldGroup("Name", new TextField() {{
            textProperty().bindBidirectional(viewModel.nameProperty());
        }}));


        // 10.0. Logic đọc .json... tự động sinh control
        for (ArtifactTemplate.FieldTemplate field : template.getFields()) {
            Node control = createControlForField(field, viewModel);
            if (control != null) {
                nodes.add(createFieldGroup(field.getName(), control));
            }
        }
        return nodes;
    }

    private Node createControlForField(ArtifactTemplate.FieldTemplate field, ArtifactViewModel viewModel) {
        // Lấy property tương ứng từ ViewModel (dùng Map)
        // Property này sẽ được ViewModel tự động tạo
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
                return textArea;

            // 11.0. Mở rộng cho Dropdown và Linker
            case "Dropdown":
            case "Linker (@ID)":
                // TODO: Triển khai (Ngày 11)
                logger.warn("Render cho {} chưa được implement.", field.getType());
                return new Label("Control [" + field.getType() + "] chưa được hỗ trợ.");

            case "Flow Builder":
                // TODO: Triển khai (Ngày 14)
                logger.warn("Render cho {} chưa được implement.", field.getType());
                return new Label("Control [" + field.getType() + "] chưa được hỗ trợ.");

            default:
                logger.warn("Loại field không xác định: {}", field.getType());
                return null;
        }
    }

    // Helper tạo nhóm Label + Control
    private Node createFieldGroup(String label, Node control) {
        VBox fieldGroup = new VBox(5);
        Label fieldLabel = new Label(label + ":");
        fieldLabel.setStyle("-fx-font-weight: bold;");
        fieldGroup.getChildren().addAll(fieldLabel, control);
        VBox.setMargin(fieldGroup, new Insets(10, 0, 0, 0));
        return fieldGroup;
    }
}