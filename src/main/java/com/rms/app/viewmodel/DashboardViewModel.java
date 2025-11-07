package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ISearchService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * "Brain" - Logic UI cho DashboardView (Kanban).
 * Tuân thủ UC-MGT-02 (Ngày 28).
 */
public class DashboardViewModel {

    private static final Logger logger = LoggerFactory.getLogger(DashboardViewModel.class);
    private final ISearchService searchService;
    private final IProjectStateService projectStateService;

    /**
     * Map (ánh xạ) có thể quan sát (Observable) lưu trữ
     * Trạng thái (String) -> Danh sách Artifacts (List<Artifact>).
     */
    private final ObservableMap<String, List<Artifact>> artifactsByStatus = FXCollections.observableHashMap();

    @Inject
    public DashboardViewModel(ISearchService searchService, IProjectStateService projectStateService) {
        this.searchService = searchService;
        this.projectStateService = projectStateService;
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

            /**
             * Cập nhật Map (trên luồng UI)
             */
            Platform.runLater(() -> {
                artifactsByStatus.clear();
                artifactsByStatus.putAll(groupedData);
                logger.info("Đã tải dữ liệu Kanban cho {} trạng thái.", groupedData.size());
            });

        } catch (Exception e) {
            logger.error("Lỗi nghiêm trọng khi tải dữ liệu Kanban", e);
            Platform.runLater(artifactsByStatus::clear);
        }
    }

    /**
     * [THÊM MỚI NGÀY 29]
     * Logic nghiệp vụ (F-MGT-03) khi BA thả một thẻ (card) vào cột mới.
     *
     * @param artifact  Artifact đã được thả
     * @param newStatus Trạng thái (Status) của cột
     */
    public void updateArtifactStatus(Artifact artifact, String newStatus) {
        projectStateService.setStatusMessage("Đang cập nhật " + artifact.getId() + "...");
        try {
            /**
             * 1. Gọi Service để thực hiện Triple-Write
             */
            searchService.updateArtifactStatus(artifact, newStatus);

            /**
             * 2. Tải lại (Refresh) toàn bộ bảng Kanban
             * (Việc này sẽ tự động cập nhật UI vì View đang lắng nghe Map)
             */
            loadKanbanData();
            projectStateService.setStatusMessage("Đã cập nhật " + artifact.getId() + " sang " + newStatus);

        } catch (IOException e) {
            logger.error("Lỗi khi cập nhật trạng thái Kanban cho {}", artifact.getId(), e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
            /**
             * Tải lại (refresh) bảng để khôi phục trạng thái cũ
             */
            loadKanbanData();
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