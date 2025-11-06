package com.rms.app.service;

import com.rms.app.model.ProjectConfig;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.io.IOException;

// Interface cho DIP (SOLID)
public interface IProjectService {

    /**
     * Tạo một cấu trúc dự án mới tại thư mục được chỉ định.
     * Tham chiếu UC-PM-01.
     */
    boolean createProject(String projectName, File directory) throws IOException;

    /**
     * Mở một dự án đã tồn tại từ thư mục.
     * Tham chiếu UC-PM-02.
     * @return ProjectConfig đã tải
     */
    ProjectConfig openProject(File directory) throws IOException;

    /**
     * Quét cấu trúc thư mục dự án và tạo TreeView.
     */
    TreeItem<String> buildProjectTree(File projectRoot);
}