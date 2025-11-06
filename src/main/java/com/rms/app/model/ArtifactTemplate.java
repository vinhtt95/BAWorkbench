package com.rms.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// POJO cho file [TemplateName].template.json
public class ArtifactTemplate {

    // Ví dụ: "Use Case"
    @JsonProperty("templateName")
    private String templateName;

    // Ví dụ: "UC"
    @JsonProperty("prefixId")
    private String prefixId;

    @JsonProperty("fields")
    private List<FieldTemplate> fields;

    // Lớp con (nested) định nghĩa một trường (field)
    public static class FieldTemplate {
        @JsonProperty("name")
        private String name; // Tên (ví dụ: "Mô tả", "Trạng thái")

        @JsonProperty("type")
        private String type; // Loại (ví dụ: "Text", "TextArea", "Dropdown")

        @JsonProperty("options")
        private Map<String, Object> options; // Tùy chọn (ví dụ: Nguồn cho Dropdown)

        // getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    // getters/setters
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getPrefixId() { return prefixId; }
    public void setPrefixId(String prefixId) { this.prefixId = prefixId; }
    public List<FieldTemplate> getFields() { return fields; }
    public void setFields(List<FieldTemplate> fields) { this.fields = fields; }
}