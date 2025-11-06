package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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

            /**
             * [SỬA LỖI NGÀY 24 - Lần 2]
             * Kiểm tra mô tả (description) do PlantUML trả về.
             * Nếu nó chứa "syntax error" (không phân biệt chữ hoa/thường),
             * chúng ta coi đó là lỗi và chủ động ném ra IOException.
             */
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
}