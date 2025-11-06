package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.IRenderService;
import com.rms.app.viewmodel.ArtifactViewModel;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * "Dumb" View Controller cho ArtifactView.fxml.
 * Chịu trách nhiệm render Form động.
 */
public class ArtifactView {

    @FXML private VBox formContainer;

    private final ArtifactViewModel viewModel;
    private final IRenderService renderService;

    private ArtifactTemplate templateToRender;
    private Artifact artifactToLoad;

    @Inject
    public ArtifactView(ArtifactViewModel viewModel, IRenderService renderService) {
        this.viewModel = viewModel;
        this.renderService = renderService;
    }

    /**
     * Hàm này sẽ được gọi bởi IViewManager TRƯỚC khi FXML được load
     * để View biết nó cần render template nào.
     */
    public void setTemplate(ArtifactTemplate template) {
        this.templateToRender = template;
    }

    /**
     * Hàm này được IViewManager gọi (nếu là mở file)
     */
    public void setArtifact(Artifact artifact) {
        this.artifactToLoad = artifact;
    }

    @FXML
    public void initialize() {
        /**
         * [SỬA LỖI] Khởi tạo ViewModel với cả template (để biết prefix)
         * và artifact (để biết data).
         */
        viewModel.initializeData(templateToRender, artifactToLoad);

        if (templateToRender != null) {
            /**
             * Bây giờ, RenderService sẽ lấy dữ liệu đã load từ ViewModel
             */
            List<Node> formNodes = renderService.renderForm(templateToRender, viewModel);
            formContainer.getChildren().addAll(formNodes);
        } else {
            formContainer.getChildren().add(new javafx.scene.control.Label("Lỗi: Không tìm thấy template."));
        }
    }
}