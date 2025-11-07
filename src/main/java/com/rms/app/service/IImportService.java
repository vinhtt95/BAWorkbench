package com.rms.app.service;

import com.rms.app.model.ArtifactTemplate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface (cho DIP) của Service Import.
 * Chịu trách nhiệm đọc và phân tích (parse) file Excel.
 * Tuân thủ Kế hoạch Ngày 34 (UC-PM-03).
 */
public interface IImportService {

    /**
     * Đọc file Excel và trả về danh sách tên các Sheet (trang tính).
     *
     * @param excelFile File .xlsx
     * @return Danh sách tên các Sheet
     * @throws IOException Nếu lỗi đọc file
     */
    List<String> loadExcelSheetNames(File excelFile) throws IOException;

    /**
     * Đọc hàng (row) đầu tiên (hàng tiêu đề) của một Sheet
     * và trả về danh sách tên các cột.
     *
     * @param excelFile File .xlsx
     * @param sheetName Tên của Sheet cần đọc
     * @return Danh sách tên các Cột (Headers)
     * @throws IOException Nếu lỗi đọc file
     */
    List<String> getSheetHeaders(File excelFile, String sheetName) throws IOException;

    /**
     * Thực thi logic Import (UC-PM-03, Bước 12.0, 13.0).
     *
     * @param excelFile     File .xlsx (Nguồn)
     * @param sheetToTypeMap Ánh xạ (Map) {SheetName -> TemplateName}
     * @param columnMapping Ánh xạ (Map) {SheetName -> {ExcelColumnName -> FieldName}}
     * @return Báo cáo (String) tóm tắt kết quả
     * @throws IOException Nếu lỗi I/O
     */
    String runImport(File excelFile,
                     Map<String, String> sheetToTypeMap,
                     Map<String, Map<String, String>> columnMapping) throws IOException;
}