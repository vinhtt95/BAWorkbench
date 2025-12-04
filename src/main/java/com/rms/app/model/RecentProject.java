package com.rms.app.model;

import java.io.Serializable;

public class RecentProject implements Serializable {
    private String name;
    private String path;
    private long lastOpened;

    public RecentProject() {}

    public RecentProject(String name, String path, long lastOpened) {
        this.name = name;
        this.path = path;
        this.lastOpened = lastOpened;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public long getLastOpened() { return lastOpened; }
    public void setLastOpened(long lastOpened) { this.lastOpened = lastOpened; }
}