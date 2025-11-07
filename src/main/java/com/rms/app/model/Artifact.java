package com.rms.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

/**
 * POJO cơ sở cho mọi đối tượng yêu cầu.
 * [CẬP NHẬT] Thêm templateId để hỗ trợ Versioning.
 * [CẬP NHẬT 2] Thêm relativePath để hỗ trợ cây thư mục đa cấp.
 */
public class Artifact implements Serializable {

    /**
     * ID phiên bản để đảm bảo tính tương thích (compatibility) khi Serializable.
     */
    private static final long serialVersionUID = 3L; // [CẬP NHẬT] Tăng SUID

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("artifactType")
    private String artifactType; // Ví dụ: "UC", "BR" (Tiền tố Logic)

    /**
     * [MỚI] Đường dẫn vật lý tương đối từ gốc dự án
     * Ví dụ: "UC/Tài Khoản/UC001.json"
     * Đây là "Source of Truth" cho vị trí cây.
     */
    @JsonProperty("relativePath")
    private String relativePath;

    /**
     * [MỚI] ID phiên bản template chính xác
     * (ví dụ: "UC_v1", "UC_v2")
     * Dùng để đảm bảo artifact luôn được render bằng đúng form
     * mà nó đã được tạo ra.
     */
    @JsonProperty("templateId")
    private String templateId;

    /**
     * Một map linh hoạt để lưu trữ tất cả các trường (fields)
     * được định nghĩa trong Form Builder (UC-CFG-01)
     */
    @JsonProperty("fields")
    private Map<String, Object> fields;

    // getters and setters...

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
}