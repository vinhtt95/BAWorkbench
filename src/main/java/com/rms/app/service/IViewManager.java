package com.rms.app.service;

import com.rms.app.model.ArtifactTemplate;
import javafx.stage.Stage;

// Service quản lý việc mở View (như `StageManager`)
public interface IViewManager {
    void initialize(Stage primaryStage);
    void openViewInNewTab(String fxmlPath, String tabTitle);
    // (Sẽ thêm openViewInNewWindow() sau)
    // Hàm để mở tab Artifact (truyền tham số)
    void openArtifactTab(ArtifactTemplate template);
}