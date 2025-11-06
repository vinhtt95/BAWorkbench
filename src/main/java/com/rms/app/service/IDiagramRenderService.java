package com.rms.app.service;

import com.rms.app.model.FlowStep;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

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

    /**
     * [THÊM MỚI NGÀY 25]
     * Chuyển đổi dữ liệu Flow (List<FlowStep>) thành mã PlantUML.
     * Tham chiếu UC-MOD-01 [vinhtt95/baworkbench/BAWorkbench-c5a6f74b866bd635fc341b1b5b0b13160f7ba9a1/Requirement/UseCases/UC-MOD-01.md]
     *
     * @param flowSteps Danh sách các bước
     * @return Chuỗi mã PlantUML
     */
    String generatePlantUmlCode(List<FlowStep> flowSteps);
}