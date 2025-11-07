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
}