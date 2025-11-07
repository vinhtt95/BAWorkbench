package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.*;
import javafx.application.Platform;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai (implementation) logic nghiệp vụ Import (UC-PM-03).
 */
@Singleton
public class ImportServiceImpl implements IImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);

    private final ITemplateService templateService;
    private final IArtifactRepository artifactRepository;
    private final IIndexService indexService;
    private final IProjectStateService projectStateService;

    @Inject
    public ImportServiceImpl(ITemplateService templateService,
                             IArtifactRepository artifactRepository,
                             IIndexService indexService,
                             IProjectStateService projectStateService) {
        this.templateService = templateService;
        this.artifactRepository = artifactRepository;
        this.indexService = indexService;
        this.projectStateService = projectStateService;
    }

    @Override
    public List<String> loadExcelSheetNames(File excelFile) throws IOException {
        List<String> sheetNames = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
        }
        return sheetNames;
    }

    @Override
    public List<String> getSheetHeaders(File excelFile, String sheetName) throws IOException {
        List<String> headers = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IOException("Không tìm thấy Sheet: " + sheetName);
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    headers.add(getStringCellValue(cell));
                }
            }
        }
        return headers;
    }

    @Override
    public String runImport(File excelFile,
                            Map<String, String> sheetToTypeMap,
                            Map<String, Map<String, String>> columnMapping) throws IOException {

        Platform.runLater(() -> projectStateService.setStatusMessage("Đang import từ Excel..."));

        int totalArtifactsCreated = 0;
        int totalErrors = 0;
        StringBuilder report = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            /**
             * Bước 12.0: Lặp (loop) qua từng Sheet đã được ánh xạ (map)
             */
            for (Map.Entry<String, String> sheetEntry : sheetToTypeMap.entrySet()) {
                String sheetName = sheetEntry.getKey();
                String templateName = sheetEntry.getValue();
                Map<String, String> fieldMap = columnMapping.get(sheetName);

                if (fieldMap == null || fieldMap.isEmpty()) {
                    logger.warn("Bỏ qua Sheet '{}': Không có ánh xạ (mapping) cột.", sheetName);
                    continue;
                }

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) continue;

                ArtifactTemplate template = templateService.loadTemplate(templateName);
                if (template == null) {
                    logger.error("Bỏ qua Sheet '{}': Không tìm thấy Template '{}'", sheetName, templateName);
                    totalErrors++;
                    report.append("Lỗi: Không tìm thấy Template '").append(templateName).append("'.\n");
                    continue;
                }

                int sheetArtifacts = 0;
                Map<String, Integer> headerIndex = mapHeaderToIndex(sheet.getRow(0));

                /**
                 * Bước 13.0: Lặp (loop) qua từng hàng (row)
                 */
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    try {
                        Artifact artifact = new Artifact();
                        artifact.setArtifactType(template.getPrefixId());
                        artifact.setFields(new HashMap<>());

                        /**
                         * Ánh xạ (map) các cột (column) vào các trường (field)
                         */
                        for (Map.Entry<String, String> mapEntry : fieldMap.entrySet()) {
                            String excelColumn = mapEntry.getKey();
                            String fieldName = mapEntry.getValue();
                            Integer columnIndex = headerIndex.get(excelColumn);

                            if (columnIndex != null) {
                                String cellValue = getStringCellValue(row.getCell(columnIndex));

                                if ("id".equalsIgnoreCase(fieldName)) {
                                    artifact.setId(cellValue);
                                } else if ("name".equalsIgnoreCase(fieldName)) {
                                    artifact.setName(cellValue);
                                } else {
                                    artifact.getFields().put(fieldName, cellValue);
                                }
                            }
                        }

                        /**
                         * Xử lý ID và Tên (Name) nếu bị thiếu
                         */
                        if (artifact.getName() == null || artifact.getName().isEmpty()) {
                            artifact.setName("Imported Artifact");
                        }
                        if (artifact.getId() == null || artifact.getId().isEmpty()) {
                            artifact.setId(template.getPrefixId() + "-" + (System.currentTimeMillis() % 100000 + i));
                        }

                        /**
                         * Bước 13.0 (Lưu):
                         * Gọi Repository, Repository sẽ tự động xử lý Triple-Write
                         * (Json, Md, và Cập nhật Index TỨC THỜI)
                         */
                        artifactRepository.save(artifact);
                        sheetArtifacts++;
                        totalArtifactsCreated++;

                    } catch (Exception e) {
                        logger.error("Lỗi khi import hàng {} (Sheet {}): {}", i, sheetName, e.getMessage());
                        totalErrors++;
                    }
                }
                report.append("Sheet '").append(sheetName).append("': Đã import ").append(sheetArtifacts).append(" đối tượng.\n");
            }
        }

        String finalReport = String.format("Hoàn tất Import. Đã tạo: %d. Lỗi: %d.\n%s", totalArtifactsCreated, totalErrors, report);
        logger.info(finalReport);

        /**
         * [QUAN TRỌNG] Kế hoạch Ngày 34 yêu cầu:
         * Sau khi import, gọi RebuildIndex
         * để đảm bảo CSDL Chỉ mục (SQLite) được đồng bộ 100%.
         */
        indexService.validateAndRebuildIndex();

        int finalTotalArtifactsCreated = totalArtifactsCreated;
        int finalTotalErrors = totalErrors;
        Platform.runLater(() -> projectStateService.setStatusMessage(String.format("Import hoàn tất (%d đã tạo, %d lỗi)", finalTotalArtifactsCreated, finalTotalErrors)));
        return finalReport;
    }

    /**
     * Helper (hàm phụ) để lấy giá trị String
     * từ một ô (cell) bất kể loại (type) của nó.
     *
     * @param cell Ô (Cell) của POI
     * @return Giá trị (String)
     */
    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * Helper (hàm phụ) tạo ánh xạ (map) {ColumnName -> ColumnIndex}.
     *
     * @param headerRow Hàng (Row) tiêu đề
     * @return Map (Ánh xạ)
     */
    private Map<String, Integer> mapHeaderToIndex(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                map.put(getStringCellValue(cell), cell.getColumnIndex());
            }
        }
        return map;
    }
}