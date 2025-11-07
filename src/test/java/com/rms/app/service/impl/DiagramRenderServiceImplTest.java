package com.rms.app.service.impl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.rms.app.config.GuiceModule;
import com.rms.app.model.FlowStep;
import com.rms.app.service.IDiagramRenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kịch bản Test cho Kế hoạch Ngày 24 & 25.
 * [vinhtt95/baworkbench/BAWorkbench-c5a6f74b866bd635fc341b1b5b0b13160f7ba9a1/Requirement/ImplementPlan.md]
 */
public class DiagramRenderServiceImplTest {

    private IDiagramRenderService diagramRenderService;

    @BeforeEach
    public void setUp() {
        Injector injector = Guice.createInjector(new GuiceModule());
        diagramRenderService = injector.getInstance(IDiagramRenderService.class);
    }

    /**
     * Test kịch bản render thành công (Happy Path)
     */
    @Test
    public void testRender_ValidPlantUML_ShouldReturnImage() {
        String validCode = "@startuml\nAlice -> Bob: Hello\n@enduml";
        try {
            BufferedImage image = diagramRenderService.render(validCode);

            assertNotNull(image, "Hình ảnh không được null");
            assertTrue(image.getWidth() > 0, "Chiều rộng hình ảnh phải lớn hơn 0");
            assertTrue(image.getHeight() > 0, "Chiều cao hình ảnh phải lớn hơn 0");

        } catch (IOException e) {
            fail("Không mong đợi lỗi IOException cho mã hợp lệ: " + e.getMessage());
        }
    }

    /**
     * Test kịch bản đầu vào rỗng
     */
    @Test
    public void testRender_EmptyCode_ShouldThrowIOException() {
        String emptyCode = "";

        IOException exception = assertThrows(IOException.class, () -> {
            diagramRenderService.render(emptyCode);
        }, "Phải ném ra IOException khi mã rỗng");

        assertTrue(exception.getMessage().contains("Mã PlantUML không được rỗng."));
    }

}