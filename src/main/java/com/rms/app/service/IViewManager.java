package com.rms.app.service;

import com.rms.app.model.Artifact; // Thêm import
import com.rms.app.model.ArtifactTemplate;
import javafx.scene.control.Tab;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Service quản lý việc mở View (như `StageManager`)
 */
public interface IViewManager {
    void initialize(Stage primaryStage);
    Tab openViewInNewTab(String fxmlPath, String tabTitle) throws IOException;

    /**
     * Mở một Tab cho Artifact MỚI (chưa có dữ liệu)
     */
    Tab openArtifactTab(ArtifactTemplate template) throws IOException;

    /**
     * [THÊM MỚI] Mở một Tab cho Artifact ĐÃ TỒN TẠI (có dữ liệu)
     *
     * @param artifact Dữ liệu artifact đã load
     * @param template Template (form) của artifact đó
     * @return Tab đã sẵn sàng để hiển thị
     * @throws IOException Nếu không thể tải FXML
     */
    Tab openArtifactTab(Artifact artifact, ArtifactTemplate template) throws IOException;
}