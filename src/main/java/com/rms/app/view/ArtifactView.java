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

import java.util.List;
import java.util.Map; // [MỚI] Import

/**
 * "Dumb" View Controller cho ArtifactView.fxml.
 * [CẬP NHẬT] Khởi tạo (initialize) ViewModel
 * bằng cách sử dụng UserData từ Tab.
 */
public class ArtifactView {

    @FXML private VBox formContainer;
    @FXML private TabPane artifactTabPane;
    @FXML private Tab diagramTab;
    @FXML private ImageView diagramImageView;

    private final ArtifactViewModel viewModel;
    private final IRenderService renderService;

    /**
     * [THAY ĐỔI] Các trường (field) này
     * giờ chỉ dùng khi MỞ (OPEN) artifact
     */
    private ArtifactTemplate templateToRender;
    private Artifact artifactToLoad;

    private boolean diagramRendered = false;

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

    @FXML
    public void initialize() {

        /**
         * [ĐÃ SỬA] Logic khởi tạo (Initialization logic)
         * đã được chuyển sang listener (trình lắng nghe)
         * để đảm bảo UserData của Tab đã sẵn sàng.
         */
        formContainer.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null && viewModel.isNotInitialized()) {
                /**
                 * Tìm (Find) Tab (Pane) cha
                 * (Đây là một hack, nhưng cần thiết
                 * để lấy UserData)
                 */
                Node tabPaneNode = formContainer.getScene().lookup("#mainTabPane");
                if (tabPaneNode instanceof TabPane) {
                    TabPane tabPane = (TabPane) tabPaneNode;
                    Tab thisTab = tabPane.getSelectionModel().getSelectedItem();

                    /**
                     * Kiểm tra xem tab này có phải là tab
                     * chúng ta đang tìm không
                     * (Nội dung (content) của tab là ScrollPane -> VBox)
                     */
                    if (thisTab != null && thisTab.getContent() == formContainer.getParent().getParent()) {
                        Object userData = thisTab.getUserData();

                        if (userData instanceof Map) {
                            /**
                             * TRƯỜNG HỢP 1: TẠO MỚI (NEW)
                             * (UserData là Map)
                             */
                            viewModel.initializeData(thisTab);
                        } else {
                            /**
                             * TRƯỜNG HỢP 2: MỞ (OPEN)
                             * (UserData là String ID
                             * HOẶC artifactToLoad đã được set)
                             */
                            viewModel.initializeData(templateToRender, artifactToLoad);
                            // Cập nhật UserData thành ID
                            // (nếu nó chưa phải)
                            if (artifactToLoad != null) {
                                thisTab.setUserData(artifactToLoad.getId());
                            }
                        }

                        /**
                         * Sau khi ViewModel được khởi tạo,
                         * render (vẽ) form
                         */
                        renderFormContent();
                    }
                }
            }
        });

        setupDiagramTabLogic();
    }

    /**
     * [MỚI] Tách (Refactor)
     * logic render (vẽ) form
     * và binding (liên kết)
     */
    private void renderFormContent() {
        ArtifactTemplate template = viewModel.getTemplate(); // Lấy (Get) template từ VM

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
     * [MỚI] Tách (Refactor) logic
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