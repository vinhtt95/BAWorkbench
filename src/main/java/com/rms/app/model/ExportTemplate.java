package com.rms.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * POJO (Model) đại diện cho một Template Xuất bản (UC-CFG-03).
 * Được lưu dưới dạng [Tên].export.template.json.
 */
public class ExportTemplate {

    /**
     * Tên của template (ví dụ: "Tài liệu SRS Chuẩn")
     */
    @JsonProperty("templateName")
    private String templateName;

    /**
     * Danh sách các chương (section) theo thứ tự
     */
    @JsonProperty("sections")
    private List<ExportTemplateSection> sections;

    // Getters and Setters

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public List<ExportTemplateSection> getSections() {
        return sections;
    }

    public void setSections(List<ExportTemplateSection> sections) {
        this.sections = sections;
    }
}