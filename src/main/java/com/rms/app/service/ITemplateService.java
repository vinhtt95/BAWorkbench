package com.rms.app.service;

import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.ExportTemplate;

import java.io.IOException;
import java.util.List;

/**
 * Interface cho DIP (SOLID)
 */
public interface ITemplateService {

    /**
     * Lưu một template (cấu hình Form) vào file .json
     * Tham chiếu UC-CFG-01 (Bước 9.0, 10.0)
     *
     * @param template Template Form
     * @throws IOException Nếu lỗi I/O
     */
    void saveTemplate(ArtifactTemplate template) throws IOException;

    /**
     * Tải một template (cấu hình Form) từ file .json
     *
     * @param templateName Tên template
     * @return Template Form
     * @throws IOException Nếu lỗi I/O
     */
    ArtifactTemplate loadTemplate(String templateName) throws IOException;

    /**
     * Quét thư mục .config và trả về tên của tất cả template (Form)
     *
     * @return Danh sách tên các template (ví dụ: "Use Case", "Task")
     * @throws IOException Nếu không thể đọc thư mục
     */
    List<String> loadAllTemplateNames() throws IOException;

    /**
     * Tìm và tải một template (Form) bằng Prefix ID của nó (ví dụ: "UC").
     *
     * @param prefix Prefix ID (ví dụ: "UC", "BR")
     * @return ArtifactTemplate tìm thấy, hoặc null nếu không tìm thấy
     * @throws IOException Nếu xảy ra lỗi đọc file
     */
    ArtifactTemplate loadTemplateByPrefix(String prefix) throws IOException;

    /**
     * [THÊM MỚI NGÀY 32]
     * Lưu một Template Xuất bản (UC-CFG-03)
     *
     * @param template Template Xuất bản
     * @throws IOException Nếu lỗi I/O
     */
    void saveExportTemplate(ExportTemplate template) throws IOException;

    /**
     * [THÊM MỚI NGÀY 32]
     * Tải (load) một Template Xuất bản
     *
     * @param templateName Tên của template
     * @return Template Xuất bản
     * @throws IOException Nếu lỗi I/O
     */
    ExportTemplate loadExportTemplate(String templateName) throws IOException;

    /**
     * [THÊM MỚI NGÀY 32]
     * Lấy danh sách tên của tất cả các Template Xuất bản
     *
     * @return Danh sách tên
     * @throws IOException Nếu lỗi I/O
     */
    List<String> loadAllExportTemplateNames() throws IOException;
}