package com.rms.app.service;

import com.rms.app.model.ProjectConfig;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.io.IOException;

/**
 * Interface cho DIP (SOLID)
 */
public interface IProjectService {

    /**
     * Tạo một cấu trúc dự án mới tại thư mục được chỉ định.
     * Tham chiếu UC-PM-01.
     *
     * @param projectName Tên dự án
     * @param directory Thư mục gốc
     * @return boolean thành công
     * @throws IOException Nếu lỗi I/O
     */
    boolean createProject(String projectName, File directory) throws IOException;

    /**
     * Mở một dự án đã tồn tại từ thư mục.
     * Tham chiếu UC-PM-02.
     *
     * @param directory Thư mục gốc
     * @return ProjectConfig đã tải
     * @throws IOException Nếu lỗi I/O
     */
    ProjectConfig openProject(File directory) throws IOException;

    /**
     * Lấy cấu hình của dự án hiện tại đang mở.
     *
     * @return ProjectConfig (có thể null nếu chưa mở dự án)
     */
    ProjectConfig getCurrentProjectConfig();

    /**
     * Lưu lại (ghi đè) file project.json
     * với trạng thái (state) của đối tượng ProjectConfig trong bộ nhớ.
     *
     * @throws IOException Nếu lỗi I/O
     */
    void saveCurrentProjectConfig() throws IOException;

    /**
     * [THÊM MỚI] Helper (hàm phụ) để cập nhật và lưu API Key.
     * (Triển khai tối thiểu cho UC-CFG-04)
     *
     * @param apiKey API Key
     * @throws IOException Nếu lỗi lưu
     */
    void saveGeminiApiKey(String apiKey) throws IOException;

    /**
     * Quét cấu trúc thư mục dự án và tạo TreeView.
     *
     * @param projectRoot Thư mục gốc
     * @return TreeItem (gốc)
     */
    TreeItem<String> buildProjectTree(File projectRoot);
}