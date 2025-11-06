package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.IRenderService;
import com.rms.app.viewmodel.ArtifactViewModel;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.List;


public class ArtifactView {

    @FXML private VBox formContainer;

    // View này cần ViewModel của chính nó VÀ RenderService
    private final ArtifactViewModel viewModel;
    private final IRenderService renderService;

    // Đây là "tham số" được truyền vào khi View được tạo
    // (Chúng ta sẽ implement logic này trong IViewManager)
    private ArtifactTemplate templateToRender;

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

    @FXML
    public void initialize() {
        if (templateToRender != null) {
            // 3.0. Hệ thống tự động sinh ra giao diện Form
            List<Node> formNodes = renderService.renderForm(templateToRender, viewModel);
            formContainer.getChildren().addAll(formNodes);
        } else {
            formContainer.getChildren().add(new javafx.scene.control.Label("Lỗi: Không tìm thấy template."));
        }
    }
}