package com.rms.app.service.impl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.rms.app.config.GuiceModule;
import com.rms.app.service.IExportService;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kịch bản Test cho Kế hoạch Ngày 31 (Tích hợp Pandoc).
 * Tuân thủ UC-PUB-01.
 */
public class ExportServiceImplTest {

    private IExportService exportService;
    private IProjectStateService projectStateService;

    /**
     * Thư mục tạm thời (Temp) được JUnit quản lý.
     */
    @TempDir
    Path tempDir;

    /**
     * Helper kiểm tra xem Pandoc có được cài đặt trên
     * máy (máy build) hay không.
     *
     * @return true nếu Pandoc có trong PATH
     */
    private boolean isPandocInstalled() {
        try {
            /**
             * [SỬA LỖI NGÀY 31] Chúng ta cũng cần kiểm tra xelatex
             * (vì đó là engine chúng ta dùng).
             */
            ProcessBuilder pbPandoc = new ProcessBuilder("pandoc", "--version");
            ProcessBuilder pbLatex = new ProcessBuilder("xelatex", "--version");

            Process p1 = pbPandoc.start();
            Process p2 = pbLatex.start();

            return p1.waitFor() == 0 && p2.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @BeforeEach
    public void setUp() {
        /**
         * Bỏ qua (skip) toàn bộ test nếu Pandoc hoặc XeLaTeX không được cài đặt (A-02).
         */
        assumeTrue(isPandocInstalled(), "Bỏ qua Test: Pandoc hoặc XeLaTeX (từ basictex) chưa được cài đặt hoặc không có trong PATH.");

        Injector injector = Guice.createInjector(new GuiceModule());
        exportService = injector.getInstance(IExportService.class);
        projectStateService = injector.getInstance(IProjectStateService.class);

        /**
         * Giả lập (Mock) việc một dự án đang được mở.
         */
        projectStateService.setCurrentProjectDirectory(tempDir.toFile());
        /**
         * Đảm bảo thư mục .config tồn tại (theo logic của ExportServiceImpl)
         */
        new File(tempDir.toFile(), ProjectServiceImpl.CONFIG_DIR).mkdirs();
    }

    /**
     * Test Kịch bản Thành công (Happy Path) (UC-PUB-01)
     * [SỬA LỖI NGÀY 31] Thêm ký tự Tiếng Việt (Unicode)
     */
    @Test
    public void testExportMarkdownToPdf_Success_WithUnicode() {
        String markdown = "# Test PDF\n\nĐây là nội dung test có dấu: ộ";
        File outputFile = new File(tempDir.toFile(), "TestOutput.pdf");

        try {
            /**
             * Thực thi
             */
            exportService.exportMarkdownToPdf(markdown, outputFile);

            /**
             * Xác minh
             */
            assertTrue(outputFile.exists(), "File PDF phải được tạo.");
            assertTrue(outputFile.length() > 0, "File PDF không được rỗng.");

            /**
             * Xác minh (Nâng cao) file temp đã bị xóa
             */
            File tempMd = new File(tempDir.toFile(), ".config/temp_srs.md");
            assertFalse(tempMd.exists(), "File Markdown trung gian phải bị xóa.");

        } catch (IOException | InterruptedException e) {
            fail("Không mong đợi lỗi khi Pandoc/XeLaTeX đã được cài đặt: " + e.getMessage());
        }
    }

    /**
     * Test Kịch bản Lỗi (Nội dung MD hỏng)
     * [SỬA LỖI NGÀY 31] Sửa lại logic test.
     * Chúng ta dùng một lệnh LaTeX không hợp lệ (\badcommand)
     * thay vì HTML hỏng.
     */
    @Test
    public void testExportMarkdownToPdf_BadMarkdown() {
        /**
         * \badcommand là một lệnh LaTeX không tồn tại
         * sẽ khiến xelatex (và pandoc) thất bại.
         */
        String badMarkdown = "Nội dung \\badcommand";
        File outputFile = new File(tempDir.toFile(), "BadOutput.pdf");

        /**
         * Xác minh rằng Service đã ném (throw)
         * một IOException (như mong đợi).
         */
        assertThrows(IOException.class, () -> {
            exportService.exportMarkdownToPdf(badMarkdown, outputFile);
        }, "Phải ném ra IOException khi Pandoc thất bại.");

        /**
         * Xác minh file đích (target) không được tạo
         */
        assertFalse(outputFile.exists(), "File PDF lỗi không được tạo.");
    }
}