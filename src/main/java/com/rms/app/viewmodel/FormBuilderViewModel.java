package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.ITemplateService;
import javafx.beans.property.*;
import com.rms.app.service.IProjectStateService;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

/**
 * "Brain" - Logic UI cho FormBuilderView
 * Đã nâng cấp để hỗ trợ Sửa (Edit) và Cột Properties.
 */
public class FormBuilderViewModel {
    private static final Logger logger = LoggerFactory.getLogger(FormBuilderViewModel.class);

    private final ITemplateService templateService;
    private final IProjectStateService projectStateService;

    // --- Properties cho Danh sách Template (MỚI) ---
    public final ObservableList<String> templateNames = FXCollections.observableArrayList();

    // --- Properties cho Template Editor ---
    private SimpleObjectProperty<ArtifactTemplate> currentTemplate = new SimpleObjectProperty<>(null);
    public final StringProperty templateName = new SimpleStringProperty();
    public final StringProperty prefixId = new SimpleStringProperty();
    public final ObservableList<ArtifactTemplate.FieldTemplate> currentFields = FXCollections.observableArrayList();

    // --- Properties cho Toolbox ---
    public final ObservableList<String> toolboxItems = FXCollections.observableArrayList(
            // Các loại trường F-CFG-02
            "Text (Single-line)",
            "Text Area (Multi-line)",
            "Dropdown",
            "Linker (@ID)",
            "Flow Builder",
            "DatePicker",
            "Figma Link",
            "BPMN Editor" // [THÊM MỚI] F-MOD-05
    );

    // --- Properties cho Cột Properties (MỚI) ---
    private SimpleObjectProperty<ArtifactTemplate.FieldTemplate> selectedField = new SimpleObjectProperty<>(null);
    public final StringProperty currentFieldName = new SimpleStringProperty();
    public final StringProperty currentFieldType = new SimpleStringProperty();

    /**
     * Biến (field) để lưu trữ listener
     * Dùng để bind (thủ công)
     */
    private ChangeListener<String> nameUpdateListener;


    @Inject
    public FormBuilderViewModel(ITemplateService templateService, IProjectStateService projectStateService) {
        this.templateService = templateService;
        this.projectStateService = projectStateService;

        /**
         * Logic binding thủ công cho Cột Properties.
         */
        selectedField.addListener((obs, oldField, newField) -> {

            // Hủy listener cũ
            if (nameUpdateListener != null) {
                currentFieldName.removeListener(nameUpdateListener);
                nameUpdateListener = null;
            }

            if (newField != null) {
                // 1. Cập nhật ViewModel TỪ Model (POJO)
                currentFieldName.set(newField.getName());
                currentFieldType.set(newField.getType()); // Loại (Type) không cho phép sửa

                // 2. Tạo listener mới để cập nhật Model (POJO) TỪ ViewModel
                nameUpdateListener = (o, oldVal, newVal) -> {
                    if (newField != null) {
                        newField.setName(newVal); // Cập nhật POJO

                        int index = currentFields.indexOf(newField);
                        if (index != -1) {
                            currentFields.set(index, newField);
                        }
                    }
                };

                // Thêm listener vào StringProperty
                currentFieldName.addListener(nameUpdateListener);

            } else {
                currentFieldName.set("");
                currentFieldType.set("");
            }
        });
    }

    /**
     * Tải tất cả tên template vào danh sách
     */
    public void loadTemplateNames() {
        try {
            templateNames.clear();
            templateNames.addAll(templateService.loadAllTemplateNames());
        } catch (IOException e) {
            logger.error("Không thể tải danh sách template", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tải dữ liệu của một template khi chọn
     *
     * @param name Tên template
     */
    public void loadTemplateForEditing(String name) {
        if (name == null) {
            currentTemplate.set(null);
            templateName.set("");
            prefixId.set("");
            currentFields.clear();
            return;
        }

        try {
            ArtifactTemplate template = templateService.loadTemplate(name);
            currentTemplate.set(template);

            templateName.set(template.getTemplateName());
            prefixId.set(template.getPrefixId());

            currentFields.setAll(template.getFields());

            selectField(null); // Bỏ chọn

        } catch (IOException e) {
            logger.error("Không thể tải template: {}", name, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tạo một template mới (trống)
     */
    public void createNewTemplate() {
        ArtifactTemplate template = new ArtifactTemplate();
        template.setTemplateName("New Template");
        template.setPrefixId("NEW");
        template.setFields(new ArrayList<>());

        currentTemplate.set(template);
        templateName.set(template.getTemplateName());
        prefixId.set(template.getPrefixId());
        currentFields.clear();

        selectField(null); // Bỏ chọn
    }

    /**
     * Được gọi bởi View khi một field trong preview được click
     *
     * @param field Field được chọn
     */
    public void selectField(ArtifactTemplate.FieldTemplate field) {
        this.selectedField.set(field);
    }

    /**
     * Trả về ReadOnlyProperty.
     */
    public ReadOnlyObjectProperty<ArtifactTemplate.FieldTemplate> selectedFieldProperty() {
        return selectedField;
    }

    /**
     * Trả về ReadOnlyProperty.
     */
    public ReadOnlyObjectProperty<ArtifactTemplate> currentTemplateProperty() {
        return currentTemplate;
    }


    /**
     * Logic nghiệp vụ khi nhấn nút Save
     */
    public void saveTemplate() {
        ArtifactTemplate template = currentTemplate.get();
        if (template == null) {
            logger.warn("Không có template nào được chọn để lưu.");
            return;
        }

        template.setTemplateName(templateName.get());
        template.setPrefixId(prefixId.get());
        template.setFields(new ArrayList<>(currentFields)); // Chuyển đổi ObservableList thành List

        try {
            templateService.saveTemplate(template);
            projectStateService.setStatusMessage("Đã lưu template: " + template.getTemplateName());
            logger.info("Lưu template thành công");

            loadTemplateNames();

        } catch (IOException e) {
            logger.error("Lỗi lưu template", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }
}