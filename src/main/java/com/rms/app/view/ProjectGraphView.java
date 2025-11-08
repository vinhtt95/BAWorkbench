package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.viewmodel.ProjectGraphViewModel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * View Controller cho ProjectGraphView.fxml (UC-MOD-02).
 * Hỗ trợ JavaBridge để cho phép double-click (drill-down).
 * Chứa Label loading và nền tối để tránh flash trắng.
 */
public class ProjectGraphView {

    private static final Logger logger = LoggerFactory.getLogger(ProjectGraphView.class);
    @FXML private WebView webView;
    @FXML private StackPane rootPane;
    @FXML private Label loadingLabel;

    private final ProjectGraphViewModel viewModel;
    private WebEngine engine;
    private JavaBridge bridgeInstance;
    private String htmlTemplate = "";

    /**
     * Lớp lồng nhau làm cầu nối JavaScript-to-Java.
     */
    public class JavaBridge {
        /**
         * Được gọi bởi JavaScript (từ graph-template.html)
         * khi người dùng double-click vào một node.
         *
         * @param nodeId ID của node (ví dụ: "UC001")
         */
        public void onNodeDoubleClick(String nodeId) {
            if (nodeId != null && !nodeId.isEmpty()) {
                Platform.runLater(() -> {
                    viewModel.openArtifact(nodeId);
                });
            }
        }
    }


    @Inject
    public ProjectGraphView(ProjectGraphViewModel viewModel) {
        this.viewModel = viewModel;
        loadTemplate();
    }

    /**
     * Tải file HTML template vào bộ nhớ.
     */
    private void loadTemplate() {
        try (InputStream is = getClass().getResourceAsStream("graph-template.html")) {
            if (is == null) {
                logger.error("KHÔNG TÌM THẤY graph-template.html!");
                htmlTemplate = "<html><body>Lỗi: Không tìm thấy graph-template.html</body></html>";
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                htmlTemplate = reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            logger.error("Không thể đọc graph-template.html", e);
            htmlTemplate = "<html><body>Lỗi: " + e.getMessage() + "</body></html>";
        }
    }

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.setStyle("-fx-background-color: -fx-base;");
        }

        /**
         * [SỬA LỖI] Ép WebView phải trong suốt
         * để hiển thị màu -fx-base của rootPane
         * trong khi chờ tải,
         * loại bỏ hoàn toàn flash trắng.
         */
        if (webView != null) {
            webView.setStyle("-fx-background-color: transparent;");
        }

        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                this.bridgeInstance = new JavaBridge();
                window.setMember("javaBridge", this.bridgeInstance);

                if (loadingLabel != null) {
                    loadingLabel.setVisible(false);
                }
            }
        });

        ChangeListener<String> dataListener = (obs, oldV, newV) -> renderGraph();
        viewModel.nodesJson.addListener(dataListener);
        viewModel.edgesJson.addListener(dataListener);

        viewModel.loadGraphData();
    }

    /**
     * Inject dữ liệu JSON vào HTML và tải nó vào WebEngine.
     */
    private void renderGraph() {
        String nodes = viewModel.nodesJson.get();
        String edges = viewModel.edgesJson.get();

        if (nodes == null || edges == null || nodes.equals("[]")) {
            return;
        }

        logger.info("Đang render Sơ đồ Quan hệ (Graph) vào WebView...");
        String finalHtml = htmlTemplate
                .replace("%%NODES_JSON%%", nodes)
                .replace("%%EDGES_JSON%%", edges);

        engine.loadContent(finalHtml);
    }
}