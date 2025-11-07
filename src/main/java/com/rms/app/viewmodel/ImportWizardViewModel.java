package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.IImportService;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ITemplateService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Brain" - Logic UI cho ImportWizardView (UC-PM-03).
 * [CẬP NHẬT] Sửa lỗi versioning, sử dụng loadLatestTemplateByName.
 */
public class ImportWizardViewModel {

    private static final Logger logger = LoggerFactory.getLogger(ImportWizardViewModel.class);
    private final IImportService importService;
    private final ITemplateService templateService;
    private final IProjectStateService projectStateService;

    private File loadedExcelFile;
    private final Map<String, List<String>> headerCache = new HashMap<>();
    private final Map<String, List<String>> fieldCache = new HashMap<>();

    /**
     * Map (Ánh xạ) {SheetName -> TemplateName}
     */
    private final Map<String, String> sheetToTypeMap = new HashMap<>();
    /**
     * Map (Ánh xạ) {SheetName -> {ExcelColumnName -> FieldName}}
     */
    private final Map<String, Map<String, String>> columnMapping = new HashMap<>();

    public final StringProperty selectedSheet = new SimpleStringProperty();
    public final ObservableList<String> sheetNames = FXCollections.observableArrayList();
    public final ObservableList<String> templateNames = FXCollections.observableArrayList();
    public final ObservableList<String> availableExcelColumns = FXCollections.observableArrayList();
    public final ObservableList<String> availableTemplateFields = FXCollections.observableArrayList();

    /**
     * Map (Ánh xạ) {FieldName -> ExcelColumnName}
     * Đây là dữ liệu cho TableView ánh xạ (mapping).
     */
    public final ObservableList<ColumnMappingModel> currentMappings = FXCollections.observableArrayList();

    public final BooleanProperty isFileLoaded = new SimpleBooleanProperty(false);
    public final StringProperty statusText = new SimpleStringProperty("Vui lòng mở một file Excel (.xlsx) để bắt đầu.");

    /**
     * POJO nội bộ cho Bảng (Table) Ánh xạ (Mapping)
     */
    public static class ColumnMappingModel {
        private final StringProperty fieldName = new SimpleStringProperty();
        private final StringProperty fieldType = new SimpleStringProperty();
        private final ObjectProperty<ComboBox<String>> excelColumnCombo = new SimpleObjectProperty<>();

        public ColumnMappingModel(String fieldName, String fieldType, ObservableList<String> excelOptions, String mappedColumn) {
            this.fieldName.set(fieldName);
            this.fieldType.set(fieldType);
            ComboBox<String> combo = new ComboBox<>(excelOptions);
            combo.setPromptText("Bỏ qua (Skip)");
            if (mappedColumn != null) {
                combo.setValue(mappedColumn);
            }
            this.excelColumnCombo.set(combo);
        }
        public String getFieldName() { return fieldName.get(); }
        public StringProperty fieldNameProperty() { return fieldName; }
        public StringProperty fieldTypeProperty() { return fieldType; }
        public ComboBox<String> getExcelColumnCombo() { return excelColumnCombo.get(); }
        public ObjectProperty<ComboBox<String>> excelColumnComboProperty() { return excelColumnCombo; }
    }


    @Inject
    public ImportWizardViewModel(IImportService importService, ITemplateService templateService, IProjectStateService projectStateService) {
        this.importService = importService;
        this.templateService = templateService;
        this.projectStateService = projectStateService;

        try {
            templateNames.addAll(templateService.loadAllTemplateNames());
        } catch (IOException e) {
            logger.error("Không thể tải (load) artifact templates", e);
        }
    }

    /**
     * Được gọi bởi View khi một file được chọn (UC-PM-03, Bước 3.0).
     *
     * @param excelFile File Excel
     */
    public void loadFile(File excelFile) {
        this.loadedExcelFile = excelFile;
        this.headerCache.clear();
        this.fieldCache.clear();
        this.sheetToTypeMap.clear();
        this.columnMapping.clear();
        this.currentMappings.clear();

        try {
            List<String> sheets = importService.loadExcelSheetNames(excelFile);
            sheetNames.setAll(sheets);
            isFileLoaded.set(true);
            statusText.set("Đã tải: " + excelFile.getName() + ". Vui lòng chọn một Sheet bên trái.");
        } catch (IOException e) {
            logger.error("Không thể đọc file Excel", e);
            statusText.set("Lỗi: " + e.getMessage());
            isFileLoaded.set(false);
        }
    }

    /**
     * Được gọi bởi View khi người dùng chọn một Sheet (UC-PM-03, Bước 6.0).
     *
     * @param sheetName Tên Sheet
     */
    public void loadSheetForMapping(String sheetName) {
        if (sheetName == null || loadedExcelFile == null) {
            currentMappings.clear();
            return;
        }

        try {
            /**
             * 1. Tải (load) các Cột (Column) của Excel (và cache lại)
             */
            if (!headerCache.containsKey(sheetName)) {
                headerCache.put(sheetName, importService.getSheetHeaders(loadedExcelFile, sheetName));
            }
            availableExcelColumns.setAll(headerCache.get(sheetName));
            ObservableList<String> comboOptions = FXCollections.observableArrayList(availableExcelColumns);
            comboOptions.add(0, null); // Cho phép "Bỏ qua"

            /**
             * 2. Lấy (Get) Template (nếu đã được ánh xạ)
             */
            String templateName = sheetToTypeMap.get(sheetName);
            if (templateName == null) {
                currentMappings.clear();
                statusText.set("Vui lòng chọn 'Loại Artifact' cho Sheet '" + sheetName + "'.");
                return;
            }
            statusText.set("Đang ánh xạ (mapping) Sheet '" + sheetName + "' vào Loại '" + templateName + "'.");

            /**
             * 3. Tải (load) các Trường (Field) của Template (và cache lại)
             */
            if (!fieldCache.containsKey(templateName)) {
                /**
                 * [SỬA LỖI] Sử dụng phiên bản mới nhất của template
                 */
                ArtifactTemplate template = templateService.loadLatestTemplateByName(templateName);
                List<String> fields = new ArrayList<>();
                fields.add("id"); // Trường (field) đặc biệt
                fields.add("name"); // Trường (field) đặc biệt
                template.getFields().forEach(f -> fields.add(f.getName()));
                fieldCache.put(templateName, fields);
            }
            availableTemplateFields.setAll(fieldCache.get(templateName));

            /**
             * 4. Tạo các hàng (row) của Bảng (Table) Ánh xạ (Mapping)
             */
            currentMappings.clear();
            Map<String, String> currentSheetMapping = columnMapping.computeIfAbsent(sheetName, k -> new HashMap<>());
            for (String fieldName : availableTemplateFields) {
                String mappedExcelColumn = currentSheetMapping.get(fieldName);
                currentMappings.add(new ColumnMappingModel(fieldName, "...", comboOptions, mappedExcelColumn));
            }

        } catch (IOException e) {
            logger.error("Không thể tải (load) chi tiết Sheet/Template", e);
            statusText.set("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Được gọi bởi View khi người dùng thay đổi ComboBox Loại Artifact.
     *
     * @param sheetName    Sheet hiện tại
     * @param templateName Template mới được chọn
     */
    public void mapSheetToType(String sheetName, String templateName) {
        if (sheetName == null) return;
        sheetToTypeMap.put(sheetName, templateName);
        /**
         * Tải (load) lại Bảng (Table) Ánh xạ (Mapping)
         */
        loadSheetForMapping(sheetName);
    }

    /**
     * Lấy (Get) TemplateName hiện tại cho một Sheet (để cập nhật ComboBox)
     *
     * @param sheetName Tên Sheet
     * @return Tên Template
     */
    public String getMappedTypeForSheet(String sheetName) {
        return sheetToTypeMap.get(sheetName);
    }

    /**
     * Bắt đầu quá trình import (luồng nền).
     */
    public void runImport() {
        if (loadedExcelFile == null) {
            statusText.set("Lỗi: Chưa mở file Excel.");
            return;
        }

        /**
         * Cập nhật map (ánh xạ) cuối cùng từ Bảng (Table) UI
         */
        for (Map.Entry<String, String> entry : sheetToTypeMap.entrySet()) {
            String sheetName = entry.getKey();
            loadSheetForMapping(sheetName); // Tải (load) các mapping cho sheet này

            Map<String, String> sheetMap = columnMapping.computeIfAbsent(sheetName, k -> new HashMap<>());
            sheetMap.clear();

            for (ColumnMappingModel model : currentMappings) {
                String selectedExcelColumn = model.getExcelColumnCombo().getValue();
                if (selectedExcelColumn != null) {
                    sheetMap.put(selectedExcelColumn, model.getFieldName());
                }
            }
        }

        /**
         * Tạo và chạy Tác vụ (Task) (UC-PM-03, Bước 11.0)
         */
        Task<String> importTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return importService.runImport(loadedExcelFile, sheetToTypeMap, columnMapping);
            }

            @Override
            protected void succeeded() {
                statusText.set(getValue());
                isFileLoaded.set(false); // Buộc (force) người dùng mở lại file
            }

            @Override
            protected void failed() {
                statusText.set("Lỗi nghiêm trọng khi Import: " + getException().getMessage());
                logger.error("Lỗi Import Task", getException());
            }
        };
        new Thread(importTask).start();
    }
}