package com.rms.app.service;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

import java.io.File;

/**
 * Service Singleton chứa trạng thái toàn cục của ứng dụng,
 * dùng để phá vỡ phụ thuộc vòng vào MainViewModel.
 */
public interface IProjectStateService {
    File getCurrentProjectDirectory();
    ObjectProperty<File> currentProjectDirectoryProperty();
    void setCurrentProjectDirectory(File file);

    StringProperty statusMessageProperty();
    void setStatusMessage(String message);
}