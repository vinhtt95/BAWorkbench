package com.rms.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Triển khai (implementation) logic nghiệp vụ Xuất bản.
 * Tuân thủ Kế hoạch Ngày 30 (UC-PUB-02).
 */
@Singleton
public class ExportServiceImpl implements IExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);

    private final ITemplateService templateService;
    private final IArtifactRepository artifactRepository;
    private final ISqliteIndexRepository indexRepository;
    private final IProjectStateService projectStateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public ExportServiceImpl(ITemplateService templateService,
                             IArtifactRepository artifactRepository,
                             ISqliteIndexRepository indexRepository,
                             IProjectStateService projectStateService) {
        this.templateService = templateService;
        this.artifactRepository = artifactRepository;
        this.indexRepository = indexRepository;
        this.projectStateService = projectStateService;
    }

    /**
     * Triển khai UC-PUB-02.
     *
     * @param outputFile            File .xlsx (đích)
     * @param templateNamesToExport Danh sách tên các Loại (ví dụ: "Use Case") cần xuất
     * @throws IOException Nếu lỗi I/O
     */
    @Override
    public void exportToExcel(File outputFile, List<String> templateNamesToExport) throws IOException {
        projectStateService.setStatusMessage("Đang xuất ra Excel...");
        logger.info("Bắt đầu xuất Excel (UC-PUB-02) ra file: {}", outputFile.getAbsolutePath());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            for (String templateName : templateNamesToExport) {
                try {
                    ArtifactTemplate template = templateService.loadTemplate(templateName);
                    if (template == null) {
                        logger.warn("Bỏ qua: Không tìm thấy template cho '{}'", templateName);
                        continue;
                    }

                    /**
                     * Bước 7.3 (UC-PUB-02): Tạo Sheet (Trang tính)
                     */
                    XSSFSheet sheet = workbook.createSheet(templateName);

                    /**
                     * Bước 7.4 & 7.5 (UC-PUB-02): Tạo Hàng Tiêu đề (Header Row)
                     */
                    List<String> headers = new ArrayList<>();
                    headers.add("ID");
                    headers.add("Name");
                    headers.addAll(template.getFields().stream()
                            .map(ArtifactTemplate.FieldTemplate::getName)
                            .collect(Collectors.toList()));

                    Row headerRow = sheet.createRow(0);
                    for (int i = 0; i < headers.size(); i++) {
                        headerRow.createCell(i).setCellValue(headers.get(i));
                    }

                    /**
                     * Bước 7.6 (UC-PUB-02): Truy vấn Chỉ mục (Index) để lấy danh sách file
                     */
                    List<Artifact> artifacts = indexRepository.getArtifactsByType(template.getPrefixId());

                    /**
                     * Bước 7.7 (UC-PUB-02): Ghi dữ liệu (từ Source of Truth)
                     */
                    int rowNum = 1;
                    for (Artifact summaryArtifact : artifacts) {
                        /**
                         * Tải (load) toàn bộ file .json (Source of Truth)
                         */
                        Artifact fullArtifact = artifactRepository.load(
                                template.getPrefixId() + File.separator + summaryArtifact.getId() + ".json");

                        Row row = sheet.createRow(rowNum++);

                        /**
                         * Ghi các cột cố định
                         */
                        row.createCell(0).setCellValue(fullArtifact.getId());
                        row.createCell(1).setCellValue(fullArtifact.getName());

                        /**
                         * Ghi các cột động (từ template)
                         */
                        Map<String, Object> fields = fullArtifact.getFields();
                        for (int i = 0; i < template.getFields().size(); i++) {
                            String fieldName = template.getFields().get(i).getName();
                            Object value = fields.get(fieldName);
                            Cell cell = row.createCell(i + 2);

                            /**
                             * Tuân thủ BR-EXPORT-01
                             */
                            writeCellValue(cell, value);
                        }
                    }
                    logger.info("Đã xử lý xong Sheet: {}", templateName);

                } catch (IOException | SQLException e) {
                    logger.error("Lỗi khi xử lý template '{}': {}", templateName, e.getMessage());
                }
            }

            /**
             * Bước 8.0 (UC-PUB-02): Lưu file
             */
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                workbook.write(out);
            }
            projectStateService.setStatusMessage("Xuất Excel thành công: " + outputFile.getName());
            logger.info("Xuất Excel thành công.");

        } catch (Exception e) {
            logger.error("Lỗi nghiêm trọng khi tạo Workbook Excel", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
            throw new IOException("Lỗi khi ghi file Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Helper (hàm phụ) để ghi giá trị vào Cell (Ô).
     * Tuân thủ BR-EXPORT-01 (Làm phẳng dữ liệu phức tạp).
     *
     * @param cell  Ô (Cell) của POI
     * @param value Đối tượng (Object) giá trị
     */
    private void writeCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            cell.setCellValue(value.toString());
        } else {
            /**
             * Làm phẳng (Flatten) FlowStep, List, v.v. thành chuỗi JSON.
             */
            try {
                cell.setCellValue(objectMapper.writeValueAsString(value));
            } catch (Exception e) {
                logger.warn("Không thể serialize trường (field) sang JSON: {}", e.getMessage());
                cell.setCellValue("[Lỗi Chuyển đổi]");
            }
        }
    }
}