package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.IRenderService;
import com.rms.app.viewmodel.ArtifactViewModel;
import javafx.beans.property.ListProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * "Dumb" View Controller cho ArtifactView.fxml.
 * [CẬP NHẬT] Khởi tạo (initialize) ViewModel
 * bằng cách sử dụng UserData từ Tab (được ViewManager inject).
 */
public class ArtifactView {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactView.class);

    @FXML private VBox formContainer;
    @FXML private TabPane artifactTabPane;
    @FXML private Tab diagramTab;
    @FXML private ImageView diagramImageView;

    private final ArtifactViewModel viewModel;
    private final IRenderService renderService;

    private ArtifactTemplate templateToRender;
    private Artifact artifactToLoad;
    private Tab myTab;
    private boolean diagramRendered = false;

    /**
     * [SỬA LỖI 2] Cờ (flag)
     * để đảm bảo form chỉ được render (vẽ) một lần.
     */
    private boolean formRendered = false;

    @Inject
    public ArtifactView(ArtifactViewModel viewModel, IRenderService renderService) {
        this.viewModel = viewModel;
        this.renderService = renderService;
    }

    /**
     * Hàm này sẽ được gọi bởi IViewManager TRƯỚC khi FXML được load
     * (CHỈ KHI MỞ ARTIFACT ĐÃ TỒN TẠI)
     *
     * @param template Template của artifact
     */
    public void setTemplate(ArtifactTemplate template) {
        this.templateToRender = template;
    }

    /**
     * Hàm này được IViewManager gọi (CHỈ KHI MỞ ARTIFACT ĐÃ TỒN TẠI)
     *
     * @param artifact Dữ liệu artifact (hoặc null)
     */
    public void setArtifact(Artifact artifact) {
        this.artifactToLoad = artifact;
    }

    /**
     * [SỬA LỖI 2] Hàm này được IViewManager gọi
     * SAU KHI FXML được load và Tab được tạo.
     * Đây là tín hiệu (signal)
     * an toàn để bắt đầu khởi tạo (initialize)
     *
     * @param tab Tab (JavaFX) mà View này thuộc về
     */
    public void setMyTab(Tab tab) {
        this.myTab = tab;

        /**
         * [SỬA LỖI 2] Di chuyển (move) toàn bộ logic
         * khởi tạo (initialization) vào đây.
         * Nó chỉ chạy MỘT LẦN khi tab được tạo.
         */
        if (viewModel.isNotInitialized()) {
            Tab thisTab = this.myTab;
            if (thisTab == null) {
                logger.error("CRITICAL: Không thể khởi tạo ArtifactView, myTab là null.");
                return;
            }

            Object userData = thisTab.getUserData();

            if (userData instanceof Map) {
                /**
                 * TRƯỜNG HỢP 1: TẠO MỚI (NEW)
                 */
                viewModel.initializeData(thisTab);
            } else {
                /**
                 * TRƯỜNG HỢP 2: MỞ (OPEN)
                 */
                viewModel.initializeData(templateToRender, artifactToLoad, thisTab);
                if (artifactToLoad != null) {
                    thisTab.setUserData(artifactToLoad.getId());
                }
            }
        }

        /**
         * [SỬA LỖI 2] Render (vẽ) form
         * NẾU nó chưa được render (vẽ).
         */
        if (!formRendered) {
            renderFormContent();
            formRendered = true;
        }
    }

    @FXML
    public void initialize() {
        /**
         * [SỬA LỖI 2] Xóa (Remove)
         * listener 'sceneProperty' không đáng tin cậy.
         * Logic khởi tạo (init logic)
         * đã được chuyển (move) sang setMyTab().
         */
        setupDiagramTabLogic();
    }

    /**
     * Tách (Refactor)
     * logic render (vẽ) form
     * và binding (liên kết)
     */
    private void renderFormContent() {
        ArtifactTemplate template = viewModel.getTemplate(); /** Lấy (Get) template từ VM */

        if (template != null) {
            List<Node> formNodes = renderService.renderForm(template, viewModel);
            formContainer.getChildren().addAll(formNodes);
        } else {
            formContainer.getChildren().add(new Label("Lỗi: Không tìm thấy template."));
        }

        /**
         * Lắng nghe (Listen) sự kiện
         * Auto-save để làm mới (refresh) sơ đồ
         */
        viewModel.nameProperty().addListener((obs, oldV, newV) -> invalidateDiagram());

        viewModel.dynamicFields.values().forEach(prop -> {
            if (prop instanceof ListProperty) {
                ObservableList<?> list = ((ListProperty<?>) prop).get();
                if (list != null) {
                    list.addListener((javafx.collections.ListChangeListener<Object>) c -> {
                        invalidateDiagram();
                    });
                }
            } else {
                prop.addListener((obs, oldV, newV) -> invalidateDiagram());
            }
        });
    }

    /**
     * Tách (Refactor) logic
     * cài đặt (setup) Tab Sơ đồ (Diagram)
     */
    private void setupDiagramTabLogic() {
        /**
         * Logic lazy-load
         */
        diagramTab.setOnSelectionChanged(event -> {
            if (diagramTab.isSelected() && !diagramRendered) {
                Image diagramImage = viewModel.generateDiagram();
                if (diagramImage != null) {
                    diagramImageView.setImage(diagramImage);
                    diagramRendered = true;
                }
            }
        });
    }


    /**
     * Đánh dấu sơ đồ là "cũ" (stale)
     * (Luồng 1.0.A1).
     */
    private void invalidateDiagram() {
        diagramRendered = false;
        diagramImageView.setImage(null);
    }
}