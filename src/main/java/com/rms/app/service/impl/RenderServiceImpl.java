package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.FlowStep;
import com.rms.app.model.ProjectConfig;
import com.rms.app.service.IRenderService;
import com.rms.app.service.IProjectService;
import com.rms.app.service.ISearchService;
import com.rms.app.view.BPMNEditorControl; // [THÊM MỚI] Import
import com.rms.app.view.FlowBuilderControl;
import com.rms.app.viewmodel.ArtifactViewModel;
import com.rms.app.viewmodel.MainViewModel; // [THÊM MỚI] Import
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Triển khai logic render Form động (UC-DEV-01)
 * và logic Autocomplete (UC-DEV-02).
 */
public class RenderServiceImpl implements IRenderService {

    private static final Logger logger = LoggerFactory.getLogger(RenderServiceImpl.class);
    private final Injector injector;
    private final ISearchService searchService;
    private final ContextMenu autocompletePopup;

    /**
     * Inject IProjectService để lấy danh sách Release (UC-MGT-03).
     */
    private final IProjectService projectService;

    /**
     * [THÊM MỚI] Inject MainViewModel (Singleton) để truyền (pass) vào BPMNEditor
     * cho chức năng drill-down (F-MOD-05).
     */
    private final MainViewModel mainViewModel;


    @Inject
    public RenderServiceImpl(Injector injector, IProjectService projectService, MainViewModel mainViewModel) {
        this.injector = injector;
        this.searchService = injector.getInstance(ISearchService.class);
        this.projectService = projectService;
        this.mainViewModel = mainViewModel; // [THÊM MỚI]
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

        if ("Flow Builder".equals(field.getType())) {
            try {
                ObservableList<FlowStep> steps = viewModel.getFlowStepProperty(field.getName());
                return loadFlowBuilderControl(steps, viewModel);
            } catch (IOException e) {
                logger.error("Không thể tải FlowBuilderControl.fxml", e);
                return new Label("Lỗi khi tải Flow Builder");
            }
        }

        /**
         * [THÊM MỚI] Logic render cho F-MOD-05
         */
        if ("BPMN Editor".equals(field.getType())) {
            try {
                StringProperty xmlProperty = viewModel.getStringProperty(field.getName());
                return loadBPMNEditorControl(xmlProperty, viewModel, mainViewModel);
            } catch (IOException e) {
                logger.error("Không thể tải BPMNEditorControl.fxml", e);
                return new Label("Lỗi khi tải BPMN Editor");
            }
        }


        switch (field.getType()) {
            case "Text (Single-line)":
                TextField textField = new TextField();
                textField.textProperty().bindBidirectional(viewModel.getStringProperty(field.getName()));
                return textField;

            case "Text Area (Multi-line)":
                TextArea textArea = new TextArea();
                textArea.setPrefRowCount(5);
                textArea.textProperty().bindBidirectional(viewModel.getStringProperty(field.getName()));
                setupAutocomplete(textArea);
                return textArea;

            case "Dropdown":
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.setItems(getDropdownOptions(field));
                comboBox.valueProperty().bindBidirectional(viewModel.getStringProperty(field.getName()));
                return comboBox;

            case "Linker (@ID)":
                TextField linkerField = new TextField();
                linkerField.textProperty().bindBidirectional(viewModel.getStringProperty(field.getName()));
                linkerField.setPromptText("Gõ @ để tìm kiếm...");
                setupAutocomplete(linkerField);
                return linkerField;

            case "DatePicker":
                DatePicker datePicker = new DatePicker();
                ObjectProperty<LocalDate> dateProperty = viewModel.getLocalDateProperty(field.getName());
                datePicker.valueProperty().bindBidirectional(dateProperty);
                return datePicker;

            /**
             * [THÊM MỚI] Triển khai (implementation)
             * Link đơn giản (UC-DEV-04)
             */
            case "Figma Link":
                TextField figmaField = new TextField();
                figmaField.textProperty().bindBidirectional(viewModel.getStringProperty(field.getName()));
                figmaField.setPromptText("Dán (paste) link Figma...");
                return figmaField;

            default:
                logger.warn("Loại field không xác định: {}", field.getType());
                return null;
        }
    }

    /**
     * Lấy các tùy chọn (options) cho ComboBox (Dropdown).
     * Hỗ trợ nguồn động (ví dụ: @Releases) cho UC-MGT-03.
     *
     * @param field Template của trường (field)
     * @return Danh sách các tùy chọn (options)
     */
    private ObservableList<String> getDropdownOptions(ArtifactTemplate.FieldTemplate field) {
        ObservableList<String> options = FXCollections.observableArrayList();

        if ("Target Release".equalsIgnoreCase(field.getName())) {
            try {
                ProjectConfig config = projectService.getCurrentProjectConfig();
                if (config != null && config.getReleases() != null) {
                    List<String> releaseNames = config.getReleases().stream()
                            .map(rel -> rel.get("id") + ": " + rel.get("name"))
                            .collect(Collectors.toList());
                    options.addAll(releaseNames);
                } else {
                    options.add("(Chưa có Release nào được cấu hình)");
                }
            } catch (Exception e) {
                logger.error("Không thể tải danh sách Release động", e);
                options.add("(Lỗi tải Release)");
            }
        } else {
            /**
             * Nguồn tĩnh (ví dụ: "Status")
             */
            options.addAll("Draft", "In Review", "Approved");
        }

        return options;
    }


    /**
     * Thêm logic Autocomplete (gợi ý @ID) vào một control (TextField hoặc TextArea).
     *
     * @param control Control (TextField, TextArea) cần thêm tính năng.
     */
    private void setupAutocomplete(final TextInputControl control) {

        /**
         * Logic Autocomplete (UC-DEV-02, Normal Flow)
         */
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
     * @param steps     Danh sách các bước quy trình
     * @param viewModel ViewModel của Artifact (để inject)
     * @return Node (control) FlowBuilder
     * @throws IOException Nếu không thể tải FXML
     */
    private Node loadFlowBuilderControl(ObservableList<FlowStep> steps, ArtifactViewModel viewModel) throws IOException {
        String fxmlPath = "/com/rms/app/view/FlowBuilderControl.fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

        FlowBuilderControl controller = injector.getInstance(FlowBuilderControl.class);
        loader.setController(controller);
        Parent controlRoot = loader.load();

        controller.setData(steps);
        controller.setViewModel(viewModel);

        return controlRoot;
    }

    /**
     * [THÊM MỚI] Tải (load) FXML control BPMNEditorControl
     * và inject (tiêm) data (dữ liệu) (XML) vào.
     *
     * @param xmlProperty       Property (thuộc tính) chứa XML
     * @param artifactViewModel ViewModel của Artifact
     * @param mainViewModel     ViewModel Chính (để drill-down)
     * @return Node (control) BPMNEditor
     * @throws IOException Nếu không thể tải FXML
     */
    private Node loadBPMNEditorControl(StringProperty xmlProperty, ArtifactViewModel artifactViewModel, MainViewModel mainViewModel) throws IOException {
        String fxmlPath = "/com/rms/app/view/BPMNEditorControl.fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

        BPMNEditorControl controller = injector.getInstance(BPMNEditorControl.class);
        loader.setController(controller);
        Parent controlRoot = loader.load();

        controller.setData(xmlProperty, artifactViewModel, mainViewModel);

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