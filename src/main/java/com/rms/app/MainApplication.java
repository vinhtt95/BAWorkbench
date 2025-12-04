package com.rms.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.rms.app.config.GuiceModule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import com.rms.app.service.IViewManager;
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

        IViewManager viewManager = injector.getInstance(IViewManager.class);
        viewManager.initialize(primaryStage);

        // Mở màn hình Welcome thay vì MainView
        viewManager.showWelcomeView();
    }

    public static void main(String[] args) {
        launch(args);
    }
}