package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.rms.app.service.IViewManager;
import com.rms.app.viewmodel.MainViewModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class ViewManagerImpl implements IViewManager {

    private static final Logger logger = LoggerFactory.getLogger(ViewManagerImpl.class);
    private final Injector injector; // Dùng để load FXML Controller
    private final MainViewModel mainViewModel; // Dùng để lấy TabPane
    private Stage primaryStage;

    @Inject
    public ViewManagerImpl(Injector injector, MainViewModel mainViewModel) {
        this.injector = injector;
        this.mainViewModel = mainViewModel;
    }

    @Override
    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public void openViewInNewTab(String fxmlPath, String tabTitle) {
        try {
            FXMLLoader loader = new FXMLLoader();
            URL fxmlLocation = getClass().getResource(fxmlPath);
            loader.setLocation(fxmlLocation);

            // Yêu cầu Guice tạo Controller (quan trọng)
            loader.setControllerFactory(injector::getInstance);

            Parent viewRoot = loader.load();

            Tab newTab = new Tab(tabTitle);
            newTab.setContent(viewRoot);

            // Thêm tab vào MainViewModel
            mainViewModel.getOpenTabs().add(newTab);
            // (Chúng ta cần cơ chế binding hoặc tham chiếu trực tiếp đến TabPane)
            // (Tạm thời add vào ViewModel, MainView sẽ phải lắng nghe)

        } catch (IOException e) {
            logger.error("Không thể tải FXML: " + fxmlPath, e);
            mainViewModel.statusMessageProperty().set("Lỗi: " + e.getMessage());
        }
    }
}