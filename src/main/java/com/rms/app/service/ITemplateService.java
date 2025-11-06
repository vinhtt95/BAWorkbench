package com.rms.app.service;

import com.rms.app.model.ArtifactTemplate;

import java.io.IOException;
import java.util.List;

/**
 * Interface cho DIP (SOLID)
 */
public interface ITemplateService {

    /**
     * Lưu một template (cấu hình Form) vào file .json
     * Tham chiếu UC-CFG-01 (Bước 9.0, 10.0)
     */
    void saveTemplate(ArtifactTemplate template) throws IOException;

    /**
     * Tải một template từ file .json
     */
    ArtifactTemplate loadTemplate(String templateName) throws IOException;

    /**
     * Quét thư mục .config và trả về tên của tất cả template
     *
     * @return Danh sách tên các template (ví dụ: "Use Case", "Task")
     * @throws IOException Nếu không thể đọc thư mục
     */
    List<String> loadAllTemplateNames() throws IOException;

    /**
     * [THÊM MỚI] Tìm và tải một template bằng Prefix ID của nó (ví dụ: "UC").
     *
     * @param prefix Prefix ID (ví dụ: "UC", "BR")
     * @return ArtifactTemplate tìm thấy, hoặc null nếu không tìm thấy
     * @throws IOException Nếu xảy ra lỗi đọc file
     */
    ArtifactTemplate loadTemplateByPrefix(String prefix) throws IOException;
}