package com.rms.app.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.rms.app.model.FlowStep;
import com.rms.app.service.IApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Triển khai (implementation) logic gọi API Gemini.
 * Tuân thủ Kế hoạch Ngày 35 (UC-DEV-03).
 */
@Singleton
public class ApiServiceImpl implements IApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiServiceImpl.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiServiceImpl() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Xây dựng (build) một prompt (câu lệnh) JSON
     * yêu cầu Gemini trả lời dưới dạng JSON (FlowStep).
     *
     * @param artifactName Tên (Name)
     * @param artifactDescription Mô tả (Description)
     * @return Chuỗi (String) JSON của Request (Yêu cầu)
     */
    private String buildGeminiRequest(String artifactName, String artifactDescription) {
        String prompt = String.format(
                "Dựa trên Tên Use Case: '%s' và Mô tả: '%s'. " +
                        "Hãy đề xuất một Luồng Sự kiện (Flow) cơ bản. " +
                        "Chỉ trả lời bằng một mảng (array) JSON hợp lệ (valid) " +
                        "tuân thủ định dạng (format) sau: " +
                        "[{\"actor\": \"Tên Actor\", \"action\": \"Hành động của actor\"}, " +
                        "{\"logicType\": \"IF\", \"actor\": \"Logic\", \"action\": \"Điều kiện IF...\", " +
                        "\"nestedSteps\": [{\"actor\": \"Actor\", \"action\": \"Hành động lồng nhau\"}]}] " +
                        "Không thêm bất kỳ giải thích hay ký tự ```json nào.",
                artifactName,
                artifactDescription
        );

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            logger.error("Không thể serialize request (yêu cầu) Gemini", e);
            return "{}";
        }
    }

    @Override
    public List<FlowStep> generateFlowFromGemini(String artifactName, String artifactDescription, String apiKey) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API Key chưa được cấu hình. Vui lòng vào Settings > API Keys.");
        }

        String requestBody = buildGeminiRequest(artifactName, artifactDescription);
        String fullUrl = GEMINI_API_URL + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        logger.info("Đang gửi (sending) yêu cầu (request) đến Gemini...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.error("Gemini API trả về lỗi (Code {}): {}", response.statusCode(), response.body());
            throw new IOException("Gemini API Error: " + response.body());
        }

        /**
         * Phân tích (Parse) JSON response (phản hồi)
         */
        return parseGeminiResponse(response.body());
    }

    /**
     * Helper (hàm phụ) phân tích (parse) JSON response (phản hồi)
     * phức tạp của Gemini.
     *
     * @param responseBody Nội dung (Body) của Response (Phản hồi)
     * @return Danh sách (List) các FlowStep
     * @throws IOException Nếu JSON bị hỏng (malformed)
     */
    private List<FlowStep> parseGeminiResponse(String responseBody) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root
                    .path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text");

            if (textNode.isMissingNode()) {
                throw new IOException("Response (Phản hồi) JSON của Gemini không hợp lệ (invalid).");
            }

            String jsonText = textNode.asText();

            /**
             * Làm sạch (Clean) các ký tự ```json nếu Gemini trả về
             */
            jsonText = jsonText.replace("```json", "").replace("```", "").trim();

            /**
             * Chuyển đổi (Convert) chuỗi (string) JSON
             * (mà Gemini trả về) thành List<FlowStep>
             */
            return objectMapper.readValue(jsonText, new TypeReference<List<FlowStep>>() {});

        } catch (Exception e) {
            logger.error("Không thể phân tích (parse) JSON response (phản hồi) từ Gemini: {}", e.getMessage());
            throw new IOException("Không thể đọc response (phản hồi) từ Gemini. " + e.getMessage());
        }
    }
}