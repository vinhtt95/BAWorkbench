package com.rms.app.model;

/**
 * POJO (Model) đại diện cho một thư mục (folder)
 * được lưu trong bảng 'folders' của CSDL Chỉ mục (Index DB).
 */
public class ProjectFolder {

    private String id;
    private String name;
    private String parentId; // (null nếu là gốc)
    private String artifactTypeScope; // (ví dụ: "UC", "BR", null nếu là thư mục tùy chỉnh)
    private String relativePath;

    // --- Getters and Setters ---

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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getArtifactTypeScope() {
        return artifactTypeScope;
    }

    public void setArtifactTypeScope(String artifactTypeScope) {
        this.artifactTypeScope = artifactTypeScope;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
}