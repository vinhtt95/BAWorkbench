package com.rms.app.viewmodel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;

// Tuân thủ MVVM [C-03]:
// - Không import javafx.scene.control.* (TextField, Button...)
// - Chỉ dùng JavaFX Properties
public class MainViewModel {

    // Ví dụ về các Properties mà View sẽ bind vào

    // Dữ liệu cho TreeView (Cột Trái)
    private final ObjectProperty<TreeItem<String>> projectRoot;

    // Dữ liệu cho TabPane (Cột Giữa)
    private final ObservableList<Tab> openTabs;

    public MainViewModel() {
        // Khởi tạo dữ liệu (tạm thời là dummy data)
        this.projectRoot = new SimpleObjectProperty<>(
                new TreeItem<>("Project Root (Chưa mở)")
        );
        this.openTabs = FXCollections.observableArrayList();

        // Thêm Tab demo
        Tab welcomeTab = new Tab("Welcome");
        welcomeTab.setContent(new javafx.scene.control.Label("Chào mừng đến với RMS v1.0"));
        this.openTabs.add(welcomeTab);
    }

    // --- Getters (và Properties) cho View binding ---

    public TreeItem<String> getProjectRoot() {
        return projectRoot.get();
    }

    public ObjectProperty<TreeItem<String>> projectRootProperty() {
        return projectRoot;
    }

    public ObservableList<Tab> getOpenTabs() {
        return openTabs;
    }
}