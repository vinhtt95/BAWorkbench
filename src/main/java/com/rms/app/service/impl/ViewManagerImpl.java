package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.rms.app.model.Artifact;
import com.rms.app.service.IViewManager;
import com.rms.app.viewmodel.MainViewModel;
import com.rms.app.model.ArtifactTemplate;
import javafx.fxml.FXMLLoader;
import com.rms.app.view.ArtifactView;
import com.rms.app.service.IProjectStateService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

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
            controller.setArtifact(artifact); /** Sẽ là null nếu tạo mới */

            loader.setController(controller);

            Parent viewRoot = loader.load(); /** Dòng này gọi initialize() */

            /**
             * Đặt tên Tab dựa trên artifact (nếu có) hoặc template (nếu mới)
             */
            String tabTitle = (artifact != null) ? artifact.getId() : "New " + template.getPrefixId();
            Tab newTab = new Tab(tabTitle);
            newTab.setContent(viewRoot);

            /**
             * [SỬA LỖI 2] Truyền (pass) tham chiếu Tab
             * vào controller của nó.
             * Hàm này giờ sẽ kích hoạt logic khởi tạo
             * (init logic) trong ArtifactView.
             */
            controller.setMyTab(newTab);

            return newTab;

        } catch (IOException e) {
            logger.error("Không thể tải FXML: " + fxmlPath, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Triển khai (implementation) logic "undock" (tháo) tab (UI-02).
     *
     * @param tab         Tab để "undock" (tháo ra)
     * @param mainTabPane TabPane gốc để "re-dock" (gắn lại) khi đóng
     */
    @Override
    public void openNewWindowForTab(Tab tab, TabPane mainTabPane) {
        if (tab == null) {
            logger.error("Attempted to undock a null tab. Operation aborted.");
            return;
        }

        Parent content = (Parent) tab.getContent();
        if (content == null) {
            logger.error("Không thể undock: Nội dung tab (tab content) bị null.");
            return;
        }

        /**
         * [SỬA LỖI 1] Gỡ bỏ (remove) tab
         * khỏi TabPane cha (parent)
         * TRƯỚC KHI hiển thị (show) cửa sổ mới.
         * Đây là hành động khắc phục lỗi IndexOutOfBoundsException.
         */
        mainTabPane.getTabs().remove(tab);

        /**
         * Gỡ bỏ (remove) nội dung (content)
         * khỏi tab TRƯỚKHI thêm nó vào Scene mới.
         */
        tab.setContent(null);

        /**
         * Tạo một Stage (Cửa sổ) mới
         */
        Stage newStage = new Stage();

        /**
         * Lấy tên tab từ graphic (Label)
         */
        try {
            if (tab.getGraphic() instanceof HBox) {
                HBox graphic = (HBox) tab.getGraphic();
                if (!graphic.getChildren().isEmpty() && graphic.getChildren().get(0) instanceof Label) {
                    newStage.setTitle(((Label) graphic.getChildren().get(0)).getText());
                }
            } else {
                newStage.setTitle(tab.getText()); // Fallback
            }
        } catch (Exception e) {
            newStage.setTitle("Tab");
        }


        /**
         * Lấy kích thước (size) của nội dung
         */
        double width = content.prefWidth(-1);
        double height = content.prefHeight(-1);
        width = (width <= 0) ? 800 : width;
        height = (height <= 0) ? 600 : height;

        /**
         * Tạo một root container MỚI (StackPane)
         */
        StackPane newRoot = new StackPane();
        newRoot.getChildren().add(content);
        Scene scene = new Scene(newRoot, width, height); /** Sử dụng newRoot */

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
         * [ĐÃ THAY ĐỔI] Đã loại bỏ logic "Re-dock".
         * Khi cửa sổ mới bị đóng, tab sẽ bị đóng vĩnh viễn.
         */
        newStage.setOnCloseRequest(event -> {
            logger.debug("Cửa sổ tab nổi đã đóng. Không re-dock.");
            // Không làm gì cả, cửa sổ cứ thế đóng.
        });

        newStage.show();
    }
}