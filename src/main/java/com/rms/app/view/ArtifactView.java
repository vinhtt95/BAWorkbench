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

/**
 * "Dumb" View Controller cho ArtifactView.fxml.
 * Chịu trách nhiệm render Form động.
 */
public class ArtifactView {

    @FXML private VBox formContainer;
    @FXML private TabPane artifactTabPane;
    @FXML private Tab diagramTab;
    @FXML private ImageView diagramImageView;

    private final ArtifactViewModel viewModel;
    private final IRenderService renderService;

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
     * để View biết nó cần render template nào.
     *
     * @param template Template của artifact
     */
    public void setTemplate(ArtifactTemplate template) {
        this.templateToRender = template;
    }

    /**
     * Hàm này được IViewManager gọi (nếu là mở file)
     *
     * @param artifact Dữ liệu artifact (hoặc null)
     */
    public void setArtifact(Artifact artifact) {
        this.artifactToLoad = artifact;
    }

    @FXML
    public void initialize() {
        viewModel.initializeData(templateToRender, artifactToLoad);

        if (templateToRender != null) {
            List<Node> formNodes = renderService.renderForm(templateToRender, viewModel);
            formContainer.getChildren().addAll(formNodes);
        } else {
            formContainer.getChildren().add(new Label("Lỗi: Không tìm thấy template."));
        }

        /**
         * Logic lazy-load, chỉ render khi tab được chọn.
         * Tuân thủ UC-MOD-01 [vinhtt95/baworkbench/BAWorkbench-c5a6f74b866bd635fc341b1b5b0b13160f7ba9a1/Requirement/UseCases/UC-MOD-01.md]
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

        /**
         * Lắng nghe sự kiện Auto-save (qua các property) để
         * tự động làm mới sơ đồ (UC-MOD-01, Luồng 1.0.A1).
         *
         * [SỬA LỖI NGÀY 27]
         * Phải lắng nghe cả sự thay đổi giá trị (cho StringProperty, ObjectProperty)
         * và sự thay đổi nội dung (cho ListProperty) để đảm bảo
         * Flow Builder (List) cũng kích hoạt việc làm mới sơ đồ.
         */
        viewModel.nameProperty().addListener((obs, oldV, newV) -> invalidateDiagram());

        viewModel.dynamicFields.values().forEach(prop -> {
            if (prop instanceof ListProperty) {
                /**
                 * Nếu là ListProperty (ví dụ: Flow Builder),
                 * chúng ta lắng nghe sự thay đổi NỘI DUNG của danh sách (list).
                 */
                ObservableList<?> list = ((ListProperty<?>) prop).get();
                if (list != null) {
                    list.addListener((javafx.collections.ListChangeListener<Object>) c -> {
                        invalidateDiagram();
                    });
                }
            } else {
                /**
                 * Nếu là Property thông thường (ví dụ: StringProperty, ObjectProperty),
                 * chúng ta lắng nghe sự thay đổi GIÁ TRỊ của property.
                 */
                prop.addListener((obs, oldV, newV) -> invalidateDiagram());
            }
        });
    }

    /**
     * Đánh dấu sơ đồ là "cũ" (stale) để nó được render lại
     * vào lần click tab tiếp theo (Luồng 1.0.A1).
     */
    private void invalidateDiagram() {
        diagramRendered = false;
        diagramImageView.setImage(null);
    }
}