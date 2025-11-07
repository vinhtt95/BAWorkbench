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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * "Brain" - Logic UI cho FormBuilderView
 * [CẬP NHẬT] Hỗ trợ Sửa, Sắp xếp, Xóa,
 * Cấu hình Thuộc tính (Options), và Versioning.
 */
public class FormBuilderViewModel {
    private static final Logger logger = LoggerFactory.getLogger(FormBuilderViewModel.class);

    private final ITemplateService templateService;
    private final IProjectStateService projectStateService;

    // --- Cột 1: Danh sách Template (Logic) ---
    public final ObservableList<String> templateNames = FXCollections.observableArrayList();

    // --- Cột 2: Trình chỉnh sửa (Editor) ---
    private final SimpleObjectProperty<ArtifactTemplate> currentTemplate = new SimpleObjectProperty<>(null);
    public final StringProperty templateName = new SimpleStringProperty();
    public final StringProperty prefixId = new SimpleStringProperty();
    public final IntegerProperty currentVersion = new SimpleIntegerProperty();
    public final StringProperty templateId = new SimpleStringProperty(); // (ví dụ: "UC_v1")

    // --- Cột 2: Danh sách Trường (Field) (Preview) ---
    public final ObservableList<ArtifactTemplate.FieldTemplate> currentFields = FXCollections.observableArrayList();

    // --- Cột 2: Hộp công cụ (Toolbox) ---
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

    // --- Cột 3: Thuộc tính (Properties) ---
    private final SimpleObjectProperty<ArtifactTemplate.FieldTemplate> selectedField = new SimpleObjectProperty<>(null);
    public final StringProperty currentFieldName = new SimpleStringProperty();
    public final StringProperty currentFieldType = new SimpleStringProperty();
    public final StringProperty currentFieldOptions = new SimpleStringProperty(); // (Cho Dropdown)

    // Listener (trình lắng nghe) binding (thủ công)
    private ChangeListener<String> nameUpdateListener;
    private ChangeListener<String> optionsUpdateListener;


    @Inject
    public FormBuilderViewModel(ITemplateService templateService, IProjectStateService projectStateService) {
        this.templateService = templateService;
        this.projectStateService = projectStateService;

        /**
         * Logic binding thủ công phức tạp cho Cột 3 (Properties).
         * Khi selectedField thay đổi, Cột 3 phải được cập nhật.
         */
        selectedField.addListener((obs, oldField, newField) -> {

            // 1. Hủy (remove) listener cũ
            if (nameUpdateListener != null) {
                currentFieldName.removeListener(nameUpdateListener);
            }
            if (optionsUpdateListener != null) {
                currentFieldOptions.removeListener(optionsUpdateListener);
            }

            if (newField != null) {
                // 2. Cập nhật ViewModel TỪ Model (POJO)
                currentFieldName.set(newField.getName());
                currentFieldType.set(newField.getType()); // Loại (Type) không cho phép sửa

                // 3. Cập nhật Tùy chọn (Options) (cho Dropdown)
                if ("Dropdown".equals(newField.getType())) {
                    if (newField.getOptions() == null) {
                        newField.setOptions(new HashMap<>());
                    }
                    // Đọc từ Map và chuyển thành String (phân tách bằng dòng mới)
                    Object source = newField.getOptions().get("source");
                    if (source instanceof String) {
                        currentFieldOptions.set((String) source);
                    } else {
                        currentFieldOptions.set("");
                    }
                } else {
                    currentFieldOptions.set(""); // Xóa nếu không phải Dropdown
                }


                // 4. Tạo listener mới để cập nhật Model (POJO) TỪ ViewModel
                nameUpdateListener = (o, oldVal, newVal) -> {
                    if (newField != null) {
                        newField.setName(newVal); // Cập nhật POJO
                        refreshSelectedField(); // Buộc (force) ListView refresh
                    }
                };

                optionsUpdateListener = (o, oldVal, newVal) -> {
                    if (newField != null && "Dropdown".equals(newField.getType())) {
                        newField.getOptions().put("source", newVal); // Cập nhật POJO
                    }
                };

                // 5. Thêm (add) listener
                currentFieldName.addListener(nameUpdateListener);
                currentFieldOptions.addListener(optionsUpdateListener);

            } else {
                // Xóa (clear) Cột 3
                currentFieldName.set("");
                currentFieldType.set("");
                currentFieldOptions.set("");
            }
        });
    }

    /**
     * Tải tất cả tên template LOGIC vào danh sách
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
     * Tải (load) dữ liệu của phiên bản MỚI NHẤT
     *
     * @param name Tên template logic (ví dụ: "Use Case")
     */
    public void loadTemplateForEditing(String name) {
        if (name == null) {
            currentTemplate.set(null);
            templateName.set("");
            prefixId.set("");
            currentVersion.set(0);
            templateId.set("");
            currentFields.clear();
            return;
        }

        try {
            ArtifactTemplate template = templateService.loadLatestTemplateByName(name);
            currentTemplate.set(template);

            templateName.set(template.getTemplateName());
            prefixId.set(template.getPrefixId());
            currentVersion.set(template.getVersion());
            templateId.set(template.getTemplateId());

            currentFields.setAll(template.getFields());

            selectField(null); // Bỏ chọn

        } catch (IOException e) {
            logger.error("Không thể tải template: {}", name, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tạo một template mới (trống) (phiên bản 1)
     */
    public void createNewTemplate() {
        ArtifactTemplate template = new ArtifactTemplate();
        template.setTemplateName("New Template");
        template.setPrefixId("NEW");
        template.setVersion(1); // Phiên bản đầu tiên
        template.setTemplateId("NEW_v1"); // ID phiên bản đầu tiên
        template.setFields(new ArrayList<>());

        currentTemplate.set(template);
        templateName.set(template.getTemplateName());
        prefixId.set(template.getPrefixId());
        currentVersion.set(template.getVersion());
        templateId.set(template.getTemplateId());
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
     * Logic nghiệp vụ khi nhấn nút "Save as New Version"
     * (Lưu như Phiên bản Mới)
     */
    public void saveAsNewVersion() {
        ArtifactTemplate oldTemplate = currentTemplate.get();
        if (oldTemplate == null) {
            logger.warn("Không có template nào được chọn để lưu.");
            return;
        }

        // Tạo một đối tượng template MỚI
        ArtifactTemplate newTemplate = new ArtifactTemplate();

        // Sao chép và tăng (increment) phiên bản
        newTemplate.setTemplateName(templateName.get());
        newTemplate.setPrefixId(prefixId.get());
        newTemplate.setVersion(oldTemplate.getVersion() + 1);
        newTemplate.setTemplateId(newTemplate.getPrefixId() + "_v" + newTemplate.getVersion());
        newTemplate.setFields(new ArrayList<>(currentFields)); // Chuyển đổi ObservableList thành List

        try {
            templateService.saveTemplate(newTemplate);
            projectStateService.setStatusMessage("Đã lưu phiên bản mới: " + newTemplate.getTemplateId());
            logger.info("Lưu template phiên bản mới thành công");

            // Tải lại (reload) danh sách (sẽ không thay đổi)
            loadTemplateNames();
            // Tải (load) lại trình chỉnh sửa (editor)
            // để trỏ đến phiên bản mới (ví dụ: v2)
            loadTemplateForEditing(newTemplate.getTemplateName());

        } catch (IOException e) {
            logger.error("Lỗi lưu template", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * [MỚI] Xóa trường (field) đã chọn
     */
    public void removeSelectedField() {
        if (selectedField.get() != null) {
            currentFields.remove(selectedField.get());
            selectField(null); // Bỏ chọn
        }
    }

    /**
     * [MỚI] Di chuyển trường (field) đã chọn lên trên
     */
    public void moveSelectedFieldUp() {
        ArtifactTemplate.FieldTemplate field = selectedField.get();
        if (field == null) return;
        int index = currentFields.indexOf(field);
        if (index > 0) {
            Collections.swap(currentFields, index, index - 1);
        }
    }

    /**
     * [MỚI] Di chuyển trường (field) đã chọn xuống dưới
     */
    public void moveSelectedFieldDown() {
        ArtifactTemplate.FieldTemplate field = selectedField.get();
        if (field == null) return;
        int index = currentFields.indexOf(field);
        if (index > -1 && index < currentFields.size() - 1) {
            Collections.swap(currentFields, index, index + 1);
        }
    }

    /**
     * Helper (hàm phụ) để buộc (force) ListView refresh (làm mới)
     * khi tên (name) thay đổi.
     */
    private void refreshSelectedField() {
        int index = currentFields.indexOf(selectedField.get());
        if (index != -1) {
            currentFields.set(index, selectedField.get());
        }
    }
}