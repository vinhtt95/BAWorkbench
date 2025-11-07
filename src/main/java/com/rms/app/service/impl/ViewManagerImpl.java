package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.rms.app.model.Artifact; // Thêm import
import com.rms.app.service.IViewManager;
import com.rms.app.viewmodel.MainViewModel;
import com.rms.app.model.ArtifactTemplate;
import javafx.fxml.FXMLLoader;
import com.rms.app.view.ArtifactView;
import com.rms.app.service.IProjectStateService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane; // [THÊM MỚI] Import
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class ViewManagerImpl implements IViewManager {

    private static final Logger logger = LoggerFactory.getLogger(ViewManagerImpl.class);
    private final Injector injector;
    private final IProjectStateService projectStateService;
    private Stage primaryStage;

    @Inject
    public ViewManagerImpl(Injector injector, IProjectStateService projectStateService) {
        this.injector = injector;
        this.projectStateService = projectStateService;
    }

    @Override
    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public Tab openViewInNewTab(String fxmlPath, String tabTitle) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader();
            URL fxmlLocation = getClass().getResource(fxmlPath);
            loader.setLocation(fxmlLocation);

            loader.setControllerFactory(injector::getInstance);

            Parent viewRoot = loader.load();

            Tab newTab = new Tab(tabTitle);
            newTab.setContent(viewRoot);

            return newTab;

        } catch (IOException e) {
            logger.error("Không thể tải FXML: " + fxmlPath, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Tab openArtifactTab(ArtifactTemplate template) throws IOException {
        /**
         * Gọi hàm mới, truyền artifact là null
         */
        return openArtifactTab(null, template);
    }

    /**
     * Triển khai hàm mở artifact đã có
     */
    @Override
    public Tab openArtifactTab(Artifact artifact, ArtifactTemplate template) throws IOException {
        String fxmlPath = "/com/rms/app/view/ArtifactView.fxml";
        try {
            FXMLLoader loader = new FXMLLoader();
            URL fxmlLocation = getClass().getResource(fxmlPath);
            loader.setLocation(fxmlLocation);

            ArtifactView controller = injector.getInstance(ArtifactView.class);

            /**
             * Truyền cả template VÀ artifact (có thể null) vào
             */
            controller.setTemplate(template);
            controller.setArtifact(artifact); // Sẽ là null nếu tạo mới

            loader.setController(controller);

            Parent viewRoot = loader.load();

            /**
             * Đặt tên Tab dựa trên artifact (nếu có) hoặc template (nếu mới)
             */
            String tabTitle = (artifact != null) ? artifact.getId() : "New " + template.getPrefixId();
            Tab newTab = new Tab(tabTitle);
            newTab.setContent(viewRoot);
            return newTab;

        } catch (IOException e) {
            logger.error("Không thể tải FXML: " + fxmlPath, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
            throw e;
        }
    }

    /**
     * [THÊM MỚI] Triển khai (implementation)
     * logic "undock" (tháo) tab (UI-02).
     *
     * @param tab         Tab để "undock" (tháo ra)
     * @param mainTabPane TabPane gốc để "re-dock" (gắn lại) khi đóng
     */
    @Override
    public void openNewWindowForTab(Tab tab, TabPane mainTabPane) {
        Parent content = (Parent) tab.getContent();
        if (content == null) return;

        /**
         * Tạo một Stage (Cửa sổ) mới
         */
        Stage newStage = new Stage();
        newStage.setTitle(tab.getText());

        /**
         * Lấy kích thước (size) của nội dung
         */
        double width = content.prefWidth(-1);
        double height = content.prefHeight(-1);
        width = (width <= 0) ? 800 : width;
        height = (height <= 0) ? 600 : height;

        Scene scene = new Scene(content, width, height);

        /**
         * Áp dụng CSS cho cửa sổ mới
         */
        try {
            URL cssLocation = getClass().getResource("/com/rms/app/view/dark-theme.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
            }
        } catch (Exception e) {
            logger.warn("Không thể áp dụng CSS cho cửa sổ mới", e);
        }

        newStage.setScene(scene);

        /**
         * Logic "Re-dock" (Gắn lại)
         */
        newStage.setOnCloseRequest(event -> {
            logger.debug("Đang re-dock (gắn lại) tab: {}", tab.getText());
            tab.setContent(content);
            mainTabPane.getTabs().add(tab);
            mainTabPane.getSelectionModel().select(tab);
        });

        newStage.show();
    }
}