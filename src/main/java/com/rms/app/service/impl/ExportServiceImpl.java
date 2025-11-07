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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Triển khai (implementation) logic nghiệp vụ Xuất bản.
 * Tuân thủ Kế hoạch Ngày 30 (UC-PUB-02) và Ngày 31 (UC-PUB-01).
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
     * Triển khai UC-PUB-02 (Ngày 30).
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

    /**
     * [CẬP NHẬT NGÀY 31]
     * Triển khai logic gọi Pandoc (UC-PUB-01, Bước 8.0 & 9.0).
     *
     * @param markdownContent Chuỗi nội dung Markdown (Nguồn)
     * @param outputFile      File .pdf (Đích)
     * @throws IOException          Nếu lỗi I/O (ghi file temp) hoặc lỗi Pandoc
     * @throws InterruptedException Nếu luồng (thread) Pandoc bị gián đoạn
     */
    @Override
    public void exportMarkdownToPdf(String markdownContent, File outputFile) throws IOException, InterruptedException {
        projectStateService.setStatusMessage("Đang tạo file PDF...");

        /**
         * Bước 8.0 (UC-PUB-01): Lưu file Markdown trung gian (temp)
         */
        File projectRoot = projectStateService.getCurrentProjectDirectory();
        if (projectRoot == null) {
            throw new IOException("Không có dự án nào đang mở.");
        }
        File configDir = new File(projectRoot, ProjectServiceImpl.CONFIG_DIR);
        Path tempMdPath = new File(configDir, "temp_srs.md").toPath();

        try {
            /**
             * [SỬA LỖI NGÀY 31] Ghi file với UTF-8
             * để đảm bảo Tiếng Việt (Unicode) được lưu đúng.
             */
            Files.writeString(tempMdPath, markdownContent, StandardCharsets.UTF_8);
            logger.info("Đã tạo file Markdown trung gian tại: {}", tempMdPath);

            /**
             * Bước 9.0 (UC-PUB-01): Gọi Pandoc
             *
             * [SỬA LỖI NGÀY 31] Thêm cờ (flag) --pdf-engine=xelatex
             * để yêu cầu Pandoc sử dụng engine 'xelatex' (đã cài
             * qua basictex) hỗ trợ Unicode (Tiếng Việt) thay vì
             * pdflatex (mặc định).
             */
            ProcessBuilder pb = new ProcessBuilder(
                    "pandoc",
                    tempMdPath.toAbsolutePath().toString(),
                    "-o",
                    outputFile.getAbsolutePath(),
                    "--pdf-engine=xelatex"
            );

            logger.info("Đang thực thi lệnh: {}", String.join(" ", pb.command()));

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                /**
                 * Xử lý Exception 1.0.E1 (UC-PUB-01)
                 */
                String error = new String(process.getErrorStream().readAllBytes());
                logger.error("Pandoc thất bại (Exit Code {}): {}", exitCode, error);
                if (error.contains("pandoc: command not found") || error.contains("No such file")) {
                    throw new IOException("Lỗi: Không tìm thấy Pandoc. Vui lòng cài đặt Pandoc và thêm vào PATH hệ thống. (UC-PUB-01, 1.0.E1)");
                }
                /**
                 * Bắt lỗi (catch) nếu xelatex cũng bị thiếu
                 */
                if (error.contains("xelatex not found")) {
                    throw new IOException("Lỗi: Không tìm thấy XeLaTeX. Vui lòng cài đặt (basictex) và cấu hình PATH. (UC-PUB-01, 1.0.E1)");
                }
                throw new IOException("Pandoc thất bại: " + error);
            }

            logger.info("Tạo file PDF thành công: {}", outputFile.getAbsolutePath());
            projectStateService.setStatusMessage("Xuất PDF thành công: " + outputFile.getName());

        } finally {
            /**
             * Bước 11.0 (UC-PUB-01): Luôn xóa file temp
             */
            try {
                Files.deleteIfExists(tempMdPath);
            } catch (IOException e) {
                logger.warn("Không thể xóa file temp: {}", tempMdPath, e);
            }
        }
    }
}