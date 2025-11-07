package com.rms.app.service;

import com.rms.app.model.FlowStep;

import java.io.IOException;
import java.util.List;

/**
 * Interface (cho DIP) của Service gọi API bên ngoài (Gemini, Figma, v.v.).
 * Tuân thủ Kế hoạch Ngày 35 (UC-DEV-03).
 */
public interface IApiService {

    /**
     * Gửi (submit) một prompt (câu lệnh) đến API Gemini
     * để đề xuất các bước (step) cho một flow.
     *
     * @param artifactName        Tên của artifact (ví dụ: "Đăng nhập người dùng")
     * @param artifactDescription Mô tả của artifact
     * @param apiKey              API Key của Google Gemini
     * @return Một danh sách (List) các FlowStep
     * @throws IOException          Nếu lỗi mạng (network)
     * @throws InterruptedException Nếu luồng (thread) bị gián đoạn
     */
    List<FlowStep> generateFlowFromGemini(String artifactName,
                                          String artifactDescription,
                                          String apiKey) throws IOException, InterruptedException;
}