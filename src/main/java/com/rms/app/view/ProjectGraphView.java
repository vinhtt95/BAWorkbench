package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.viewmodel.ProjectGraphViewModel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
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
 * "Dumb" View Controller cho ProjectGraphView.fxml (UC-MOD-02).
 * Thêm JavaBridge để hỗ trợ double-click (drill-down).
 */
public class ProjectGraphView {

    private static final Logger logger = LoggerFactory.getLogger(ProjectGraphView.class);
    @FXML private WebView webView;
    private final ProjectGraphViewModel viewModel;
    private WebEngine engine;

    /**
     * [SỬA LỖI] Thêm một tham chiếu mạnh (strong reference)
     * đến đối tượng JavaBridge để ngăn Bộ dọn rác (GC)
     * xóa nó.
     */
    private JavaBridge bridgeInstance;

    private String htmlTemplate = "";

    /**
     * Lớp (class) lồng nhau (nested)
     * làm cầu nối (bridge) JavaScript-to-Java.
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
                /**
                 * [SỬA LỖI 1] Phải chạy trên luồng JavaFX chính
                 * vì nó sẽ sửa đổi Scene Graph (mở cửa sổ mới).
                 */
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
     * Tải (load) file HTML template (mẫu) vào bộ nhớ (memory)
     * chỉ một lần.
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
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        /**
         * Thiết lập cầu nối (bridge) JS-to-Java
         */
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                /**
                 * [SỬA LỖI] Tạo và lưu trữ một tham chiếu mạnh
                 * (strong reference) đến cầu nối (bridge)
                 * để ngăn GC dọn dẹp nó.
                 */
                this.bridgeInstance = new JavaBridge();
                window.setMember("javaBridge", this.bridgeInstance);
            }
        });


        /**
         * Tạo các listener (trình lắng nghe)
         * để inject (tiêm) JSON khi nó sẵn sàng.
         */
        ChangeListener<String> dataListener = (obs, oldV, newV) -> renderGraph();
        viewModel.nodesJson.addListener(dataListener);
        viewModel.edgesJson.addListener(dataListener);

        /**
         * Kích hoạt (Trigger) tải (load) dữ liệu (data)
         */
        viewModel.loadGraphData();
    }

    /**
     * Inject (Tiêm) dữ liệu (data) JSON vào HTML
     * và tải (load) nó vào WebEngine.
     */
    private void renderGraph() {
        String nodes = viewModel.nodesJson.get();
        String edges = viewModel.edgesJson.get();

        if (nodes == null || edges == null || nodes.equals("[]")) {
            return;
        }

        logger.info("Đang render (vẽ) Sơ đồ Quan hệ (Graph) vào WebView...");
        String finalHtml = htmlTemplate
                .replace("%%NODES_JSON%%", nodes)
                .replace("%%EDGES_JSON%%", edges);

        engine.loadContent(finalHtml);
    }
}