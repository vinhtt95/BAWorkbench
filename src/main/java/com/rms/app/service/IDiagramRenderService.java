package com.rms.app.service;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Interface (cho DIP) của Service Render Sơ đồ.
 * Chịu trách nhiệm chuyển đổi mã PlantUML thành hình ảnh.
 * Tham chiếu Kế hoạch Ngày 24 (Giai đoạn 6).
 */
public interface IDiagramRenderService {

    /**
     * Render một chuỗi mã PlantUML thành một hình ảnh (BufferedImage).
     *
     * @param plantUmlCode Chuỗi mã PlantUML (ví dụ: "@startuml\na->b\n@enduml")
     * @return BufferedImage của sơ đồ
     * @throws IOException Nếu thư viện PlantUML không thể render mã
     */
    BufferedImage render(String plantUmlCode) throws IOException;
}