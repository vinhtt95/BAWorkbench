package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.RecentProject;
import com.rms.app.service.IGlobalConfigService;
import com.rms.app.service.IProjectService;
import com.rms.app.service.IViewManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class WelcomeViewModel {
    private static final Logger logger = LoggerFactory.getLogger(WelcomeViewModel.class);

    private final IGlobalConfigService globalConfigService;
    private final IProjectService projectService; // Để dùng logic tạo/mở dự án cũ nếu cần

    public final ObservableList<RecentProject> recentProjects = FXCollections.observableArrayList();

    @Inject
    public WelcomeViewModel(IGlobalConfigService globalConfigService, IProjectService projectService) {
        this.globalConfigService = globalConfigService;
        this.projectService = projectService;
    }

    public void loadRecentProjects() {
        List<RecentProject> projects = globalConfigService.getRecentProjects();
        recentProjects.setAll(projects);
    }

    public void addToRecent(File projectDir) {
        if (projectDir != null && projectDir.exists()) {
            globalConfigService.addRecentProject(projectDir.getName(), projectDir.getAbsolutePath());
        }
    }
}