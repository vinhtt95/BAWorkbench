package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rms.app.model.FlowStep;
import com.rms.app.service.IDiagramRenderService;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.DiagramDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Triển khai (implementation) logic nghiệp vụ Render Sơ đồ.
 * Sử dụng thư viện PlantUML (Java) để render.
 * Tham chiếu Kế hoạch Ngày 24 (Giai đoạn 6).
 */
@Singleton
public class DiagramRenderServiceImpl implements IDiagramRenderService {

    private static final Logger logger = LoggerFactory.getLogger(DiagramRenderServiceImpl.class);

    @Inject
    public DiagramRenderServiceImpl() {
    }

    @Override
    public BufferedImage render(String plantUmlCode) throws IOException {
        if (plantUmlCode == null || plantUmlCode.isEmpty()) {
            throw new IOException("Mã PlantUML không được rỗng.");
        }

        try (ByteArrayOutputStream pngStream = new ByteArrayOutputStream()) {

            SourceStringReader reader = new SourceStringReader(plantUmlCode);
            DiagramDescription desc = reader.outputImage(pngStream, 0);

            String description = (desc != null) ? desc.getDescription() : "";
            if (desc == null || description.toLowerCase().contains("syntax error")) {
                throw new IOException("PlantUML không thể render mã. Mã có thể bị lỗi cú pháp.");
            }

            byte[] pngData = pngStream.toByteArray();
            if (pngData == null || pngData.length == 0) {
                throw new IOException("PlantUML đã tạo ra một stream rỗng (empty stream).");
            }

            try (ByteArrayInputStream inStream = new ByteArrayInputStream(pngData)) {
                BufferedImage image = ImageIO.read(inStream);

                if (image == null) {
                    throw new IOException("Lỗi không xác định. ImageIO không thể đọc stream PNG.");
                }
                return image;
            }

        } catch (IOException e) {
            logger.error("Lỗi I/O khi render PlantUML: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Lỗi không xác định khi render PlantUML: {}", e.getMessage(), e);
            throw new IOException("Lỗi không xác định khi render PlantUML: " + e.getMessage(), e);
        }
    }

    /**
     * [THÊM MỚI NGÀY 25]
     * Triển khai logic sinh mã PlantUML từ FlowSteps
     */
    @Override
    public String generatePlantUmlCode(List<FlowStep> flowSteps) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("!theme vibrant\n\n");
        sb.append("start\n\n");

        generateRecursiveSteps(sb, flowSteps);

        sb.append("\nstop\n");
        sb.append("@enduml\n");
        return sb.toString();
    }

    /**
     * Helper đệ quy để xử lý các bước, bao gồm cả IF-THEN lồng nhau.
     *
     * @param sb    Đối tượng StringBuilder để xây dựng chuỗi
     * @param steps Danh sách các bước (có thể lồng nhau)
     */
    private void generateRecursiveSteps(StringBuilder sb, List<FlowStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }

        for (FlowStep step : steps) {
            String logicType = step.getLogicType();

            if ("IF".equals(logicType)) {
                sb.append("if (").append(escapeString(step.getAction())).append(") then (yes)\n");

                if (step.getNestedSteps() != null) {
                    generateRecursiveSteps(sb, step.getNestedSteps());
                }

                sb.append("endif\n");
            } else {
                sb.append("|").append(escapeString(step.getActor())).append("|\n");
                sb.append(":").append(escapeString(step.getAction())).append(";\n");
            }
        }
    }

    /**
     * Helper làm sạch chuỗi cho PlantUML (loại bỏ các ký tự đặc biệt).
     *
     * @param text Chuỗi đầu vào
     * @return Chuỗi đã được làm sạch
     */
    private String escapeString(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(":", "")
                .replace(";", "")
                .replace("\n", " ");
    }
}