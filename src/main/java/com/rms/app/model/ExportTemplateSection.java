package com.rms.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * POJO (Model) đại diện cho một "Chương" (Section)
 * bên trong một ExportTemplate (UC-CFG-03).
 */
public class ExportTemplateSection {

    /**
     * Tiêu đề của chương (ví dụ: "1. Giới thiệu", "2. Use Cases")
     */
    @JsonProperty("title")
    private String title;

    /**
     * Loại chương: "Static" (Tĩnh) hoặc "Dynamic" (Động)
     */
    @JsonProperty("type")
    private String type;

    /**
     * Nội dung (dùng nếu type="Static")
     */
    @JsonProperty("content")
    private String content;

    /**
     * Truy vấn (dùng nếu type="Dynamic")
     * (Ví dụ: { "artifactType": "UC", "status": "Approved" })
     */
    @JsonProperty("query")
    private Map<String, String> query;

    /**
     * Định dạng hiển thị (dùng nếu type="Dynamic")
     * (Ví dụ: "Table" hoặc "FullContent")
     */
    @JsonProperty("displayFormat")
    private String displayFormat;

    // Getters and Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public void setQuery(Map<String, String> query) {
        this.query = query;
    }

    public String getDisplayFormat() {
        return displayFormat;
    }

    public void setDisplayFormat(String displayFormat) {
        this.displayFormat = displayFormat;
    }
}