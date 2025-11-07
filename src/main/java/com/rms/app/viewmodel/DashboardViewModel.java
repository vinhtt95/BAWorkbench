package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.service.ISearchService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * "Brain" - Logic UI cho DashboardView (Kanban).
 * Tuân thủ UC-MGT-02 (Ngày 28).
 */
public class DashboardViewModel {

    private static final Logger logger = LoggerFactory.getLogger(DashboardViewModel.class);
    private final ISearchService searchService;

    /**
     * Map (ánh xạ) có thể quan sát (Observable) lưu trữ
     * Trạng thái (String) -> Danh sách Artifacts (List<Artifact>).
     */
    private final ObservableMap<String, List<Artifact>> artifactsByStatus = FXCollections.observableHashMap();

    @Inject
    public DashboardViewModel(ISearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Tải (load) tất cả các artifact, được nhóm theo trạng thái,
     * từ Lớp Chỉ mục (thông qua SearchService).
     */
    public void loadKanbanData() {
        try {
            /**
             * Gọi Service để lấy dữ liệu đã được xử lý (F-MGT-02)
             */
            Map<String, List<Artifact>> groupedData = searchService.getArtifactsGroupedByStatus();

            artifactsByStatus.clear();
            artifactsByStatus.putAll(groupedData);

            logger.info("Đã tải dữ liệu Kanban cho {} trạng thái.", groupedData.size());

        } catch (Exception e) {
            logger.error("Lỗi nghiêm trọng khi tải dữ liệu Kanban", e);
            artifactsByStatus.clear();
        }
    }

    /**
     * Cung cấp dữ liệu (Map) cho View (DashboardView)
     *
     * @return Map có thể quan sát (ObservableMap)
     */
    public ObservableMap<String, List<Artifact>> getArtifactsByStatus() {
        return artifactsByStatus;
    }
}