package com.rms.app.service;

import com.rms.app.model.RecentProject;
import java.util.List;

public interface IGlobalConfigService {
    List<RecentProject> getRecentProjects();
    void addRecentProject(String name, String path);
    void saveConfig();
}