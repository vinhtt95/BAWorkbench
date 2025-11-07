package com.rms.app.view;

import com.rms.app.viewmodel.ArtifactViewModel;
import com.rms.app.viewmodel.MainViewModel;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * "Dumb" View Controller cho BPMNEditorControl.fxml.
 * Control này quản lý một WebView để tải thư viện bpmn-js,
 * cho phép BA tạo Sơ đồ Quy trình (F-MOD-05).
 */
public class BPMNEditorControl {

    private static final Logger logger = LoggerFactory.getLogger(BPMNEditorControl.class);

    @FXML
    private WebView webView;

    private WebEngine engine;
    private StringProperty bpmnXmlProperty;
    private ArtifactViewModel artifactViewModel;
    private MainViewModel mainViewModel;

    private boolean editorReady = false;
    private String initialXmlData = null;

    /**
     * Chuỗi BPMN XML mặc định cho một sơ đồ trống.
     */
    private static final String EMPTY_BPMN_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
            "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" " +
            "xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" " +
            "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">" +
            "<bpmn:process id=\"Process_1\" isExecutable=\"false\">" +
            "<bpmn:startEvent id=\"StartEvent_1\"/>" +
            "</bpmn:process>" +
            "<bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">" +
            "<bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Process_1\">" +
            "<bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">" +
            "<dc:Bounds x=\"179\" y=\"159\" width=\"36\" height=\"36\"/>" +
            "</bpmndi:BPMNShape>" +
            "</bpmndi:BPMNPlane>" +
            "</bpmndi:BPMNDiagram>" +
            "</bpmn:definitions>";

    /**
     * Lớp cầu nối (bridge) JavaScript, cho phép JS gọi lại mã Java.
     */
    public class JavaBridge {
        /**
         * Được gọi bởi bpmn-js khi trình chỉnh sửa đã sẵn sàng (ready).
         */
        public void onEditorReady() {
            logger.info("bpmn-js editor is ready. Loading XML...");
            editorReady = true;
            if (initialXmlData != null && !initialXmlData.isEmpty()) {
                loadXmlIntoEditor(initialXmlData);
            } else {
                loadXmlIntoEditor(EMPTY_BPMN_XML);
            }
        }

        /**
         * Được gọi bởi bpmn-js khi nội dung XML thay đổi.
         *
         * @param newXml Chuỗi XML (đã mã hóa Base64) từ trình chỉnh sửa.
         */
        public void onXmlChanged(String newXml) {
            /**
             * Giải mã (Decode) Base64
             */
            String decodedXml = new String(Base64.getDecoder().decode(newXml), StandardCharsets.UTF_8);

            /**
             * Cập nhật StringProperty, việc này sẽ kích hoạt Auto-save
             */
            if (bpmnXmlProperty != null && !bpmnXmlProperty.get().equals(decodedXml)) {
                bpmnXmlProperty.set(decodedXml);
            }
        }

        /**
         * Được gọi bởi bpmn-js khi người dùng double-click vào một Task
         * có chứa ID (ví dụ: @UC001) trong tên của nó. (F-MOD-05 Drill-down)
         *
         * @param elementId ID của đối tượng (ví dụ: "Task_123")
         * @param elementName Tên của đối tượng (ví dụ: "Thực hiện @UC001")
         */
        public void onElementDrillDown(String elementId, String elementName) {
            logger.info("Drill-down request received for: {} ({})", elementId, elementName);
            if (mainViewModel != null && elementName != null && elementName.contains("@")) {
                /**
                 * Trích xuất (Extract) @ID đầu tiên tìm thấy
                 */
                String artifactId = extractArtifactId(elementName);
                if (artifactId != null) {
                    logger.info("Navigating to artifact: {}", artifactId);
                    mainViewModel.openArtifactById(artifactId);
                }
            }
        }

        /**
         * Helper (hàm phụ) trích xuất (extract) @ID từ một chuỗi.
         *
         * @param text Chuỗi (String) đầu vào
         * @return @ID (ví dụ: "UC001") hoặc null
         */
        private String extractArtifactId(String text) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("@([A-Za-z0-9_\\-]+)").matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }
    }


    /**
     * Được gọi bởi JavaFX sau khi các trường (field) FXML được inject (tiêm).
     */
    @FXML
    public void initialize() {
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        /**
         * [SỬA LỖI] Thêm trình xử lý (handler) lỗi
         * để bắt các lỗi JavaScript và hiển thị chúng trong console Java.
         */
        engine.getLoadWorker().exceptionProperty().addListener((obs, oldVal, newVal) -> {
            logger.error("WebView Error:", newVal);
        });
        engine.setOnError(event -> {
            logger.error("WebView Engine Error: " + event.getMessage());
        });

        /**
         * Thiết lập cầu nối (bridge) Java-to-JavaScript
         */
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new JavaBridge());
            }
        });

        /**
         * [SỬA LỖI] Tải (load) file HTML
         * thay vì loadContent().
         * Điều này thiết lập base URL chính xác
         * để các file lib/ cục bộ có thể được tìm thấy.
         */
        URL templateUrl = getClass().getResource("bpmn-template.html");
        if (templateUrl == null) {
            logger.error("CRITICAL: bpmn-template.html not found!");
            engine.loadContent("<html><body>CRITICAL: bpmn-template.html not found!</body></html>");
        } else {
            engine.load(templateUrl.toExternalForm());
        }
    }

    /**
     * Được gọi bởi RenderService để inject (tiêm) dữ liệu (data) và các ViewModel.
     *
     * @param bpmnXmlProperty     Property (thuộc tính) chứa XML
     * @param artifactViewModel   ViewModel của Artifact (cha)
     * @param mainViewModel       ViewModel chính (để drill-down)
     */
    public void setData(StringProperty bpmnXmlProperty, ArtifactViewModel artifactViewModel, MainViewModel mainViewModel) {
        this.bpmnXmlProperty = bpmnXmlProperty;
        this.artifactViewModel = artifactViewModel;
        this.mainViewModel = mainViewModel;
        this.initialXmlData = bpmnXmlProperty.get();

        /**
         * Không cần gọi loadXmlIntoEditor ở đây.
         * Cầu nối (bridge) onEditorReady() sẽ tự động
         * lấy (pull) 'initialXmlData' khi nó sẵn sàng.
         */
    }

    /**
     * Tải (load) XML vào trình chỉnh sửa (editor) bpmn-js.
     *
     * @param xml Chuỗi (String) XML
     */
    private void loadXmlIntoEditor(String xml) {
        try {
            /**
             * Mã hóa (Encode) Base64 để truyền (pass) an toàn vào JavaScript
             */
            String encodedXml = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
            engine.executeScript(String.format("loadBpmnXml('%s');", encodedXml));
        } catch (Exception e) {
            logger.error("Không thể tải (load) XML vào bpmn-js", e);
        }
    }
}