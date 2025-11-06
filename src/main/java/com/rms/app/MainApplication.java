package com.rms.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.rms.app.config.GuiceModule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApplication extends Application {

    private Injector injector;

    @Override
    public void init() throws Exception {
        this.injector = Guice.createInjector(new GuiceModule());
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();

        // SỬA LỖI: (Theo góp ý của bạn)
        // Thay đổi đường dẫn FXML từ tuyệt đối sang tương đối (relative)
        // để nạp từ "com/rms/app" (vị trí của class này)
        URL fxmlLocation = getClass().getResource("view/MainView.fxml");
        loader.setLocation(fxmlLocation);

        loader.setControllerFactory(injector::getInstance);

        Parent root = loader.load();

        primaryStage.setTitle("Requirements Management System (RMS) v1.0");

        Scene scene = new Scene(root, 1280, 720);

        // SỬA LỖI: (Theo góp ý của bạn)
        // Thay đổi đường dẫn CSS sang tương đối
        URL cssLocation = getClass().getResource("view/dark-theme.css");
        if (cssLocation != null) {
            scene.getStylesheets().add(cssLocation.toExternalForm());
        } else {
            System.err.println("Không tìm thấy file dark-theme.css");
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}