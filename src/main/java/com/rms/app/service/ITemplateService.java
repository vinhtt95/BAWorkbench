package com.rms.app.service;

import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.ExportTemplate;

import java.io.IOException;
import java.util.List;

/**
 * Interface cho DIP (SOLID)
 * [CẬP NHẬT] Hỗ trợ Versioning cho Form Template
 */
public interface ITemplateService {

    /**
     * Lưu một template (cấu hình Form) vào file .json
     * (Sử dụng template.getTemplateId() làm tên file).
     * Tham chiếu UC-CFG-01 (Bước 9.0, 10.0)
     *
     * @param template Template Form
     * @throws IOException Nếu lỗi I/O
     */
    void saveTemplate(ArtifactTemplate template) throws IOException;

    /**
     * Tải (load) một phiên bản template (cấu hình Form) CỤ THỂ
     * bằng ID của nó (ví dụ: "UC_v1").
     *
     * @param templateId ID phiên bản của template (ví dụ: "UC_v1")
     * @return Template Form
     * @throws IOException Nếu lỗi I/O
     */
    ArtifactTemplate loadTemplateById(String templateId) throws IOException;

    /**
     * Quét thư mục .config và trả về tên LOGIC
     * của tất cả template (Form).
     * (Ví dụ: "Use Case", "Task" - loại bỏ các phiên bản trùng lặp)
     *
     * @return Danh sách tên LOGIC của các template
     * @throws IOException Nếu không thể đọc thư mục
     */
    List<String> loadAllTemplateNames() throws IOException;

    /**
     * Tìm (find) phiên bản MỚI NHẤT của một template
     * bằng Tên Logic (templateName) của nó.
     *
     * @param templateName Tên logic (ví dụ: "Use Case")
     * @return Phiên bản mới nhất của ArtifactTemplate
     * @throws IOException Nếu xảy ra lỗi đọc file
     */
    ArtifactTemplate loadLatestTemplateByName(String templateName) throws IOException;


    /**
     * Tìm (find) phiên bản MỚI NHẤT của một template
     * bằng Prefix ID (tiền tố ID) của nó.
     *
     * @param prefix Prefix ID (ví dụ: "UC", "BR")
     * @return Phiên bản mới nhất của ArtifactTemplate
     * @throws IOException Nếu xảy ra lỗi đọc file
     */
    ArtifactTemplate loadLatestTemplateByPrefix(String prefix) throws IOException;

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