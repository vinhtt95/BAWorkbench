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
 * [CẬP NHẬT NGÀY 27 - SỬA LỖI GĐ 2]
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
    /**
     * [SỬA LỖI] Vẫn là SimpleObjectProperty
     */
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
            "DatePicker" // [THÊM MỚI NGÀY 27] Hỗ trợ UC-MGT-01
    );

    // --- Properties cho Cột Properties (MỚI) ---
    /**
     * [SỬA LỖI] Vẫn là SimpleObjectProperty
     */
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
         * [SỬA LỖI] Logic binding thủ công cho Cột Properties.
         * Khi selectedField (POJO) thay đổi:
         * 1. Hủy listener cũ (nếu có).
         * 2. Cập nhật currentFieldName (StringProperty) từ POJO (newField.getName()).
         * 3. Thêm listener mới: Khi currentFieldName (StringProperty) thay đổi -> Cập nhật POJO (newField.setName()).
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
                    // Phải kiểm tra lại newField vì nó nằm trong closure
                    if (newField != null) {
                        newField.setName(newVal); // Cập nhật POJO

                        // [SỬA LỖI] Phải refresh lại List (Hack)
                        // Vì List không biết bên trong FieldTemplate đã thay đổi
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
     * [MỚI] Tải tất cả tên template vào danh sách
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
     * [MỚI] Tải dữ liệu của một template khi chọn
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

            // Tải dữ liệu vào các properties
            templateName.set(template.getTemplateName());
            prefixId.set(template.getPrefixId());

            // [SỬA LỖI] Phải tạo danh sách (List) các POJO mới
            // mà POJO này phải hỗ trợ JavaFX (FieldTemplate đã không làm)
            // Tạm thời chấp nhận rủi ro khi dùng trực tiếp
            currentFields.setAll(template.getFields());

            selectField(null); // Bỏ chọn

        } catch (IOException e) {
            logger.error("Không thể tải template: {}", name, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * [MỚI] Tạo một template mới (trống)
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
     * [MỚI] Được gọi bởi View khi một field trong preview được click
     *
     * @param field Field được chọn
     */
    public void selectField(ArtifactTemplate.FieldTemplate field) {
        this.selectedField.set(field);
    }

    /**
     * [SỬA LỖI] Trả về ReadOnlyProperty.
     * Cách sửa là chỉ cần return `selectedField` vì `SimpleObjectProperty`
     * đã implement (thực thi) `ReadOnlyObjectProperty`.
     */
    public ReadOnlyObjectProperty<ArtifactTemplate.FieldTemplate> selectedFieldProperty() {
        return selectedField;
    }

    /**
     * [SỬA LỖI] Tương tự, chỉ cần return `currentTemplate`.
     */
    public ReadOnlyObjectProperty<ArtifactTemplate> currentTemplateProperty() {
        return currentTemplate;
    }


    /**
     * [CẬP NHẬT] Logic nghiệp vụ khi nhấn nút Save
     */
    public void saveTemplate() {
        ArtifactTemplate template = currentTemplate.get();
        if (template == null) {
            logger.warn("Không có template nào được chọn để lưu.");
            return;
        }

        // Cập nhật dữ liệu từ Properties về Model
        template.setTemplateName(templateName.get());
        template.setPrefixId(prefixId.get());
        template.setFields(new ArrayList<>(currentFields)); // Chuyển đổi ObservableList thành List

        try {
            // 8.0. BA nhấn "Save"
            templateService.saveTemplate(template);
            projectStateService.setStatusMessage("Đã lưu template: " + template.getTemplateName());
            logger.info("Lưu template thành công");

            // Tải lại danh sách (nếu là template mới hoặc đổi tên)
            loadTemplateNames();

        } catch (IOException e) {
            logger.error("Lỗi lưu template", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }
}