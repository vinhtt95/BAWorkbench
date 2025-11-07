package com.rms.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

/**
 * POJO cơ sở cho mọi đối tượng yêu cầu.
 * [SỬA LỖI NGÀY 29] Triển khai (implements) Serializable
 * để cho phép đối tượng này được truyền (pass) vào Dragboard (Kanban).
 */
public class Artifact implements Serializable {

    /**
     * ID phiên bản để đảm bảo tính tương thích (compatibility) khi Serializable.
     */
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("artifactType")
    private String artifactType; // Ví dụ: "UC", "BR"

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

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
}