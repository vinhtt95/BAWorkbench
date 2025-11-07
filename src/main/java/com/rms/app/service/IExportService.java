package com.rms.app.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Interface (cho DIP) của Service Xuất bản (Publication).
 * Chịu trách nhiệm xuất dữ liệu ra các định dạng (Excel, PDF, v.v.).
 * Tuân thủ Kế hoạch Ngày 30 (UC-PUB-02).
 */
public interface IExportService {

    /**
     * Xuất dữ liệu dự án (cho các loại đã chọn) ra file Excel.
     * Tuân thủ UC-PUB-02.
     *
     * @param outputFile            File .xlsx (đích)
     * @param templateNamesToExport Danh sách tên các Loại (ví dụ: "Use Case") cần xuất
     * @throws IOException Nếu lỗi I/O hoặc lỗi Apache POI
     */
    void exportToExcel(File outputFile, List<String> templateNamesToExport) throws IOException;

    /**
     * Tích hợp Pandoc (UC-PUB-01).
     * Chuyển đổi một chuỗi Markdown thành file PDF.
     *
     * @param markdownContent Chuỗi nội dung Markdown (Nguồn)
     * @param outputFile      File .pdf (Đích)
     * @throws IOException          Nếu lỗi I/O (ghi file temp) hoặc lỗi Pandoc
     * @throws InterruptedException Nếu luồng (thread) Pandoc bị gián đoạn
     */
    void exportMarkdownToPdf(String markdownContent, File outputFile) throws IOException, InterruptedException;

    /**
     * Điều phối (orchestrate) toàn bộ quá trình xuất bản PDF/DOCX.
     * Tuân thủ UC-PUB-01.
     *
     * @param outputFile         File .pdf hoặc .docx (Đích)
     * @param exportTemplateName Tên của Template Xuất bản (ví dụ: "SRS Template Chuẩn")
     * @param releaseIdFilter    ID của Release (ví dụ: "REL001", hoặc null nếu "Không lọc")
     * @throws IOException          Nếu lỗi I/O, lỗi truy vấn, hoặc lỗi Pandoc
     * @throws InterruptedException Nếu luồng (thread) Pandoc bị gián đoạn
     */
    void exportToDocument(File outputFile, String exportTemplateName, String releaseIdFilter) throws IOException, InterruptedException;
}