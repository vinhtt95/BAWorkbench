package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.FlowStep;
import com.rms.app.service.IRenderService;
import com.rms.app.view.FlowBuilderControl;
import com.rms.app.viewmodel.ArtifactViewModel;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// Triển khai logic render Form
public class RenderServiceImpl implements IRenderService {

    private static final Logger logger = LoggerFactory.getLogger(RenderServiceImpl.class);
    private final Injector injector;

    @Inject
    public RenderServiceImpl(Injector injector) {
        this.injector = injector;
    }

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
                var stringPropDrop = (StringProperty) viewModel.getFieldProperty(field.getName());
                ComboBox<String> comboBox = new ComboBox<>();
                // TODO: Đọc 'options' từ field.getOptions() để đổ dữ liệu
                comboBox.getItems().addAll("Option 1", "Option 2");
                comboBox.valueProperty().bindBidirectional(stringPropDrop);
                return comboBox;

            case "Linker (@ID)":
                var stringPropLink = (StringProperty) viewModel.getFieldProperty(field.getName());
                // TODO: (Ngày 16/17) Implement control Autocomplete tùy chỉnh
                // Tạm thời render như TextField
                TextField linkerField = new TextField();
                linkerField.textProperty().bindBidirectional(stringPropLink);
                linkerField.setPromptText("Gõ @ để tìm kiếm...");
                return linkerField;

            // 14.0. Tích hợp Flow Builder vào FormRenderer
            case "Flow Builder":
                try {
                    ObservableList<FlowStep> steps = viewModel.getFlowStepProperty(field.getName());
                    return loadFlowBuilderControl(steps);
                } catch (IOException e) {
                    logger.error("Không thể tải FlowBuilderControl.fxml", e);
                    return new Label("Lỗi khi tải Flow Builder");
                }

            default:
                logger.warn("Loại field không xác định: {}", field.getType());
                return null;
        }
    }

    // Helper (Ngày 14)
    private Node loadFlowBuilderControl(ObservableList<FlowStep> steps) throws IOException {
        String fxmlPath = "/com/rms/app/view/FlowBuilderControl.fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

        // Yêu cầu Guice tạo Controller
        loader.setControllerFactory(injector::getInstance);
        Parent controlRoot = loader.load();

        // Lấy controller và truyền dữ liệu (list) vào
        FlowBuilderControl controller = loader.getController();
        controller.setData(steps);

        return controlRoot;
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