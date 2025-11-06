package com.rms.app.service;

import javafx.stage.Stage;

// Service quản lý việc mở View (như `StageManager`)
public interface IViewManager {
    void initialize(Stage primaryStage);
    void openViewInNewTab(String fxmlPath, String tabTitle);
    // (Sẽ thêm openViewInNewWindow() sau)
}