package com.rms.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// Tuân thủ CodingConvention.md:
// - POJO không import JavaFX
// - Dùng annotation Jackson
public class ProjectConfig {

    @JsonProperty("projectName")
    private String projectName;

    @JsonProperty("version")
    private String version = "1.0";

    @JsonProperty("releases")
    private List<Map<String, String>> releases; // Sẽ dùng cho UC-CFG-02

    /**
     * [THÊM MỚI] Sẽ dùng cho UC-CFG-04 / UC-DEV-03
     */
    @JsonProperty("geminiApiKey")
    private String geminiApiKey;

    // getters and setters
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Map<String, String>> getReleases() {
        return releases;
    }

    public void setReleases(List<Map<String, String>> releases) {
        this.releases = releases;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }
}