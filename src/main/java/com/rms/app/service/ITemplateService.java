package com.rms.app.service;

import com.rms.app.model.ArtifactTemplate;

import java.io.IOException;

// Interface cho DIP (SOLID)
public interface ITemplateService {

    /**
     * Lưu một template (cấu hình Form) vào file .json
     * Tham chiếu UC-CFG-01 (Bước 9.0, 10.0)
     */
    void saveTemplate(ArtifactTemplate template) throws IOException;

    /**
     * Tải một template từ file .json
     */
    ArtifactTemplate loadTemplate(String templateName) throws IOException;

    // (Sẽ thêm các hàm khác như loadAllTemplates(), deleteTemplate()...)
}