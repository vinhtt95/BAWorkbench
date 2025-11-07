package com.rms.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable; // [THÊM MỚI] Import
import java.util.List;
import java.util.Map;

/**
 * POJO cho file [TemplateID].form.template.json
 * [CẬP NHẬT] Thêm version và templateId để hỗ trợ Versioning.
 */
public class ArtifactTemplate implements Serializable { // [THÊM MỚI] Implement Serializable (cho an toàn)

    /**
     * [MỚI] ID phiên bản để đảm bảo tính tương thích (compatibility) khi Serializable.
     */
    private static final long serialVersionUID = 1L;


    /**
     * ID duy nhất cho phiên bản template này (ví dụ: "UC_v1")
     * Đây cũng là tên file (ví dụ: UC_v1.form.template.json)
     */
    @JsonProperty("templateId")
    private String templateId;

    /**
     * Tên logic, có thể hiển thị (ví dụ: "Use Case")
     * (Tất cả các phiên bản của "Use Case" đều có cùng templateName)
     */
    @JsonProperty("templateName")
    private String templateName;

    /**
     * Tiền tố logic (ví dụ: "UC")
     */
    @JsonProperty("prefixId")
    private String prefixId;

    /**
     * Số phiên bản (ví dụ: 1, 2, 3)
     */
    @JsonProperty("version")
    private int version;


    @JsonProperty("fields")
    private List<FieldTemplate> fields;

    /**
     * Lớp con (nested) định nghĩa một trường (field)
     * [CẬP NHẬT] Thêm hỗ trợ cho Options
     * [SỬA LỖI] Phải implement Serializable để hỗ trợ Kéo-Thả (Drag-and-Drop)
     */
    public static class FieldTemplate implements Serializable {

        /**
         * [MỚI] ID phiên bản để đảm bảo tính tương thích (compatibility) khi Serializable.
         * Cần thiết cho Drag-and-Drop (Kéo-Thả) trong FormBuilder.
         */
        private static final long serialVersionUID = 1L;


        @JsonProperty("name")
        private String name; // Tên (ví dụ: "Mô tả", "Trạng thái")

        @JsonProperty("type")
        private String type; // Loại (ví dụ: "Text", "TextArea", "Dropdown")

        /**
         * Tùy chọn (ví dụ: Nguồn cho Dropdown)
         * Ví dụ: { "source": ["Option 1", "Option 2"] }
         */
        @JsonProperty("options")
        private Map<String, Object> options;

        // getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    // getters/setters
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getPrefixId() { return prefixId; }
    public void setPrefixId(String prefixId) { this.prefixId = prefixId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public List<FieldTemplate> getFields() { return fields; }
    public void setFields(List<FieldTemplate> fields) { this.fields = fields; }
}