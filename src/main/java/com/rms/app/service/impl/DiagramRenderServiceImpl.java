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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Triển khai (implementation) logic nghiệp vụ Render Sơ đồ.
 * Sử dụng thư viện PlantUML (Java) để render.
 * Tham chiếu Kế hoạch Ngày 24 (Giai đoạn 6).
 */
@Singleton
public class DiagramRenderServiceImpl implements IDiagramRenderService {

    private static final Logger logger = LoggerFactory.getLogger(DiagramRenderServiceImpl.class);
    private static final String ERROR_MESSAGE = "PlantUML không thể render mã. Mã có thể bị lỗi cú pháp.";

    @Inject
    public DiagramRenderServiceImpl() {
    }

    @Override
    public BufferedImage render(String plantUmlCode) throws IOException {
        if (plantUmlCode == null || plantUmlCode.isEmpty()) {
            throw new IOException("Mã PlantUML không được rỗng.");
        }

        /**
         * [THEO YÊU CẦU] In (log) mã PlantUML ra console.
         * Bạn có thể xem mã này trong console log của ứng dụng
         * để debug xem cú pháp đã đúng hay chưa.
         */
        logger.info("--- Mã PlantUML được sinh ra (bắt đầu) ---");
//        logger.info(plantUmlCode);
        logger.info("--- Mã PlantUML được sinh ra (kết thúc) ---");

        try (ByteArrayOutputStream pngStream = new ByteArrayOutputStream()) {

            SourceStringReader reader = new SourceStringReader(plantUmlCode);
            DiagramDescription desc = reader.outputImage(pngStream, 0);

            String description = (desc != null) ? desc.getDescription() : "";
            if (desc == null || (description != null && description.toLowerCase().contains("syntax error"))) {
                logger.error("Lỗi cú pháp PlantUML. Mã được sinh ra đã được log ở trên.");
                throw new IOException(ERROR_MESSAGE);
            }

            byte[] pngData = pngStream.toByteArray();
            if (pngData == null || pngData.length == 0) {
                throw new IOException("PlantUML đã tạo ra một stream rỗng (empty stream).");
            }

            try (ByteArrayInputStream inStream = new ByteArrayInputStream(pngData)) {
                BufferedImage image = ImageIO.read(inStream);

                if (image == null) {
                    throw new IOException(ERROR_MESSAGE);
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
     * [CẬP NHẬT NGÀY 25 - SỬA LỖI LẦN 3]
     * Triển khai logic sinh mã PlantUML từ FlowSteps
     *
     * SỬA LỖI: Tuân thủ cú pháp Sơ đồ Hoạt động:
     * 1. Quét (Scan) tất cả actor
     * 2. Khai báo (Define) tất cả swimlane
     * 3. Bắt đầu (start)
     * 4. Chuyển đổi (Switch) swimlane và in hành động (action)
     */
    @Override
    public String generatePlantUmlCode(List<FlowStep> flowSteps) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n\n");

        // --- Giai đoạn 1: Quét (Scan) ---
        // Dùng LinkedHashSet để giữ thứ tự và loại bỏ trùng lặp.
        Set<String> actors = new LinkedHashSet<>();
        collectActors(flowSteps, actors);

        // --- Giai đoạn 2: Khai báo (Define) ---
        // Khai báo TẤT CẢ các swimlane TRƯỚC KHI "start"
        if (actors.isEmpty()) {
            logger.warn("Không tìm thấy actor nào trong flow. Sơ đồ có thể render không chính xác.");
        } else {
            for (String actor : actors) {
                sb.append("|").append(escapeString(actor)).append("|\n");
            }
        }

        // --- Giai đoạn 3: Bắt đầu (Start) ---
        sb.append("\nstart\n\n");

        // --- Giai đoạn 4: Chuyển đổi (Switch) và Render ---
        // Truyền vào một "actor" (trạng thái) rỗng ban đầu.
        generateRecursiveSteps(sb, flowSteps, "");

        sb.append("\nstop\n");
        sb.append("@enduml\n");
        return sb.toString();
    }

    /**
     * [SỬA LỖI] Helper đệ quy để THU THẬP tất cả các tên actor
     *
     * @param steps Danh sách các bước
     * @param actors Set (tập hợp) để lưu trữ tên actor
     */
    private void collectActors(List<FlowStep> steps, Set<String> actors) {
        if (steps == null) return;
        for (FlowStep step : steps) {
            // "Logic" (từ IF) không phải là một swimlane
            if (step.getActor() != null && !step.getActor().isEmpty() && !"Logic".equalsIgnoreCase(step.getActor())) {
                actors.add(step.getActor());
            }

            // Quét đệ quy vào các bước lồng nhau
            if ("IF".equals(step.getLogicType())) {
                if (step.getNestedSteps() != null) {
                    collectActors(step.getNestedSteps(), actors);
                }
            }
        }
    }

    /**
     * Helper đệ quy để RENDER các bước.
     * [SỬA LỖI LẦN 3]: Helper này "stateful", nó ghi nhớ actor hiện tại.
     *
     * @param sb           Đối tượng StringBuilder để xây dựng chuỗi
     * @param steps        Danh sách các bước (có thể lồng nhau)
     * @param currentActor Actor (swimlane) hiện tại đang active
     * @return Actor (swimlane) cuối cùng được sử dụng (để duy trì trạng thái)
     */
    private String generateRecursiveSteps(StringBuilder sb, List<FlowStep> steps, String currentActor) {
        if (steps == null || steps.isEmpty()) {
            return currentActor; // Trả về trạng thái actor không đổi
        }

        String lastActor = currentActor; // Ghi nhớ actor hiện tại

        for (FlowStep step : steps) {
            String logicType = step.getLogicType();
            String actor = step.getActor();
            String action = step.getAction();

            if ("IF".equals(logicType)) {
                // Xử lý IF (không cần swimlane)
                sb.append("if (").append(escapeString(action)).append(") then (yes)\n");

                if (step.getNestedSteps() != null) {
                    // Khi vào một khối IF, hãy tiếp tục với actor hiện tại
                    // và cập nhật lại actor nếu nó thay đổi bên trong IF
                    lastActor = generateRecursiveSteps(sb, step.getNestedSteps(), lastActor);
                }

                sb.append("endif\n");
            } else {
                // Xử lý hành động (action) thông thường
                if (actor != null && !actor.isEmpty() && !actor.equalsIgnoreCase("Logic") && !actor.equals(lastActor)) {
                    // [SỬA LỖI] Chỉ "switch" (chuyển) swimlane
                    // NẾU actor của bước NÀY khác với actor của bước TRƯỚC.
                    sb.append("|").append(escapeString(actor)).append("|\n");
                    lastActor = actor; // Cập nhật trạng thái actor
                }
                // Định nghĩa hành động
                sb.append(":").append(escapeString(action)).append(";\n");
            }
        }
        return lastActor; // Trả về actor cuối cùng đã sử dụng
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
        // [CẬP NHẬT] Loại bỏ các ký tự có thể gây lỗi cú pháp PlantUML
        return text.replace(":", "")
                .replace(";", "")
                .replace("!", "")
                .replace("|", "")
                .replace("(", "")
                .replace(")", "")
                .replace("[", "")
                .replace("]", "")
                .replace("{", "")
                .replace("}", "")
                .replace("\n", " ")
                .trim();
    }
}