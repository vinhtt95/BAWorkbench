package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.viewmodel.ProjectGraphViewModel;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * "Dumb" View Controller cho ProjectGraphView.fxml (UC-MOD-02).
 */
public class ProjectGraphView {

    private static final Logger logger = LoggerFactory.getLogger(ProjectGraphView.class);
    @FXML private WebView webView;
    private final ProjectGraphViewModel viewModel;
    private WebEngine engine;

    private String htmlTemplate = "";

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