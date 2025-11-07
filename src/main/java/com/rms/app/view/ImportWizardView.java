package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.viewmodel.ImportWizardViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * "Dumb" View Controller cho ImportWizardView.fxml (UC-PM-03).
 */
public class ImportWizardView {

    private static final Logger logger = LoggerFactory.getLogger(ImportWizardView.class);
    private final ImportWizardViewModel viewModel;

    @FXML private Label statusLabel;
    @FXML private Button openFileButton;
    @FXML private BorderPane wizardPane;
    @FXML private ListView<String> sheetListView;
    @FXML private ComboBox<String> templateComboBox;
    @FXML private TableView<ImportWizardViewModel.ColumnMappingModel> mappingTableView;
    @FXML private TableColumn<ImportWizardViewModel.ColumnMappingModel, String> fieldColumn;
    @FXML private TableColumn<ImportWizardViewModel.ColumnMappingModel, ComboBox<String>> excelColumn;
    @FXML private Button importButton;

    @Inject
    public ImportWizardView(ImportWizardViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        /**
         * 1. Binding (Liên kết) Trạng thái (State)
         */
        statusLabel.textProperty().bind(viewModel.statusText);
        wizardPane.visibleProperty().bind(viewModel.isFileLoaded);
        importButton.disableProperty().bind(viewModel.isFileLoaded.not());

        /**
         * 2. Binding (Liên kết) Cột 1 (Sheet List)
         */
        sheetListView.setItems(viewModel.sheetNames);
        sheetListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSheet, newSheet) -> {
            viewModel.selectedSheet.set(newSheet);
            /**
             * Tải (load) ComboBox ánh xạ (map) loại (type)
             */
            templateComboBox.setValue(viewModel.getMappedTypeForSheet(newSheet));
            /**
             * Tải (load) Bảng (Table) ánh xạ (map) cột (column)
             */
            viewModel.loadSheetForMapping(newSheet);
        });

        /**
         * 3. Binding (Liên kết) Cột 2 (Ánh xạ (Mapping))
         */
        templateComboBox.setItems(viewModel.templateNames);
        templateComboBox.valueProperty().addListener((obs, oldType, newType) -> {
            if (newType != null) {
                viewModel.mapSheetToType(viewModel.selectedSheet.get(), newType);
            }
        });

        /**
         * 4. Binding (Liên kết) Bảng (Table) Ánh xạ (Mapping) Cột (Column)
         */
        fieldColumn.setCellValueFactory(cellData -> cellData.getValue().fieldNameProperty());
        excelColumn.setCellValueFactory(cellData -> cellData.getValue().excelColumnComboProperty());
        mappingTableView.setItems(viewModel.currentMappings);
    }

    /**
     * Xử lý sự kiện nhấn nút "Mở File Excel..."
     */
    @FXML
    private void handleOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Mở File Excel (.xlsx)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = fileChooser.showOpenDialog(openFileButton.getScene().getWindow());

        if (file != null) {
            viewModel.loadFile(file);
        }
    }

    /**
     * Xử lý sự kiện nhấn nút "Bắt đầu Import"
     */
    @FXML
    private void handleRunImport() {
        viewModel.runImport();
    }
}