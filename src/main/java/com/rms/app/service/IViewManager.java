package com.rms.app.service;

import com.rms.app.model.ArtifactTemplate;
import javafx.scene.control.Tab;
import javafx.stage.Stage;

import java.io.IOException;

// Service quản lý việc mở View (như `StageManager`)
public interface IViewManager {
    void initialize(Stage primaryStage);
    Tab openViewInNewTab(String fxmlPath, String tabTitle) throws IOException; // SỬA
    Tab openArtifactTab(ArtifactTemplate template) throws IOException; // SỬA
}