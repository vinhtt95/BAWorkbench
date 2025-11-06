package com.rms.app.service.impl;

import com.rms.app.service.IProjectStateService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;

public class ProjectStateServiceImpl implements IProjectStateService {

    private final ObjectProperty<File> currentProjectDirectory = new SimpleObjectProperty<>(null);
    private final StringProperty statusMessage = new SimpleStringProperty("Sẵn sàng.");

    @Override
    public File getCurrentProjectDirectory() {
        return currentProjectDirectory.get();
    }

    @Override
    public ObjectProperty<File> currentProjectDirectoryProperty() {
        return currentProjectDirectory;
    }

    @Override
    public void setCurrentProjectDirectory(File file) {
        currentProjectDirectory.set(file);
    }

    @Override
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    @Override
    public void setStatusMessage(String message) {
        statusMessage.set(message);
    }
}