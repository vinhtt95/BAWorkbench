package com.rms.app.viewmodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ISearchService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * "Brain" - Logic UI cho ProjectGraphView (UC-MOD-02).
 * Tuân thủ Kế hoạch Ngày 36.
 */
public class ProjectGraphViewModel {

    private static final Logger logger = LoggerFactory.getLogger(ProjectGraphViewModel.class);
    private final ISearchService searchService;
    private final IProjectStateService projectStateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Các chuỗi (string) JSON sẽ được inject (tiêm) vào WebView
     */
    public final StringProperty nodesJson = new SimpleStringProperty("[]");
    public final StringProperty edgesJson = new SimpleStringProperty("[]");

    @Inject
    public ProjectGraphViewModel(ISearchService searchService, IProjectStateService projectStateService) {
        this.searchService = searchService;
        this.projectStateService = projectStateService;
    }

    /**
     * Tải (load) dữ liệu (data) Node (Nút) và Edge (Cạnh)
     * từ CSDL Chỉ mục (Index DB) (thông qua Service).
     */
    public void loadGraphData() {
        projectStateService.setStatusMessage("Đang tải (loading) dữ liệu Sơ đồ Quan hệ...");

        Task<Map<String, List<Map<String, String>>>> graphTask = new Task<>() {
            @Override
            protected Map<String, List<Map<String, String>>> call() throws Exception {
                /**
                 * Gọi Service trên luồng nền (background thread)
                 */
                return searchService.getGraphData();
            }

            @Override
            protected void succeeded() {
                Map<String, List<Map<String, String>>> data = getValue();
                try {
                    /**
                     * Chuyển đổi (Convert) data (dữ liệu)
                     * thành JSON string trên luồng FX
                     */
                    nodesJson.set(objectMapper.writeValueAsString(data.get("nodes")));
                    edgesJson.set(objectMapper.writeValueAsString(data.get("edges")));
                    logger.info("Đã tải (load) {} nodes và {} edges cho đồ thị.", data.get("nodes").size(), data.get("edges").size());
                    projectStateService.setStatusMessage("Đã tải (load) Sơ đồ Quan hệ thành công.");
                } catch (Exception e) {
                    failed();
                }
            }

            @Override
            protected void failed() {
                logger.error("Không thể tải (load) hoặc serialize dữ liệu đồ thị", getException());
                projectStateService.setStatusMessage("Lỗi: " + getException().getMessage());
            }
        };
        new Thread(graphTask).start();
    }
}