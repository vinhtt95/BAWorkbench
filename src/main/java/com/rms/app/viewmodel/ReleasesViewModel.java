package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.ProjectConfig;
import com.rms.app.service.IIndexService;
import com.rms.app.service.IProjectService;
import com.rms.app.service.IProjectStateService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * "Brain" - Logic UI cho ReleasesView.
 * Quản lý logic nghiệp vụ cho UC-CFG-02 (Quản lý Phiên bản).
 */
public class ReleasesViewModel {

    private static final Logger logger = LoggerFactory.getLogger(ReleasesViewModel.class);
    private final IProjectService projectService;
    private final IProjectStateService projectStateService;
    private final IIndexService indexService;

    /**
     * Danh sách hiển thị trên TableView.
     */
    private final ObservableList<ReleaseModel> releasesList = FXCollections.observableArrayList();

    /**
     * Release đang được chọn (để binding với Form).
     */
    private final ObjectProperty<ReleaseModel> selectedRelease = new SimpleObjectProperty<>(null);

    /**
     * Model (POJO) bên trong ViewModel để binding
     */
    public static class ReleaseModel {
        final StringProperty id = new SimpleStringProperty();
        final StringProperty name = new SimpleStringProperty();
        final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();

        public ReleaseModel(String id, String name, LocalDate date) {
            this.id.set(id);
            this.name.set(name);
            this.date.set(date);
        }

        public String getId() { return id.get(); }
        public StringProperty idProperty() { return id; }

        /**
         * Bổ sung getter (phương thức truy cập) cho Tên (Name).
         *
         * @return Tên (Name) của Release.
         */
        public String getName() { return name.get(); }

        public StringProperty nameProperty() { return name; }
        public ObjectProperty<LocalDate> dateProperty() { return date; }

        /**
         * Chuyển đổi Model (View) sang Model (Data).
         *
         * @return Map (ánh xạ) dữ liệu
         */
        public Map<String, String> toMap() {
            Map<String, String> map = new HashMap<>();
            map.put("id", id.get());
            map.put("name", name.get());
            map.put("date", (date.get() != null) ? date.get().toString() : "");
            return map;
        }

        /**
         * Chuyển đổi Model (Data) sang Model (View).
         *
         * @param map Map (ánh xạ) dữ liệu
         * @return ReleaseModel (Mô hình Dạng xem)
         */
        public static ReleaseModel fromMap(Map<String, String> map) {
            LocalDate date = null;
            try {
                date = LocalDate.parse(map.getOrDefault("date", ""));
            } catch (DateTimeParseException e) {
                /**
                 * Bỏ qua nếu date không hợp lệ
                 */
            }
            return new ReleaseModel(map.get("id"), map.get("name"), date);
        }
    }

    @Inject
    public ReleasesViewModel(IProjectService projectService, IProjectStateService projectStateService, IIndexService indexService) {
        this.projectService = projectService;
        this.projectStateService = projectStateService;
        this.indexService = indexService;
    }

    /**
     * Tải danh sách Release từ ProjectConfig.
     */
    public void loadReleases() {
        releasesList.clear();
        ProjectConfig config = projectService.getCurrentProjectConfig();
        if (config != null && config.getReleases() != null) {
            config.getReleases().forEach(map -> releasesList.add(ReleaseModel.fromMap(map)));
        }
        logger.info("Đã tải {} releases.", releasesList.size());
    }

    /**
     * Lưu danh sách Release (đã cập nhật) vào ProjectConfig.
     *
     * @throws IOException Nếu lưu file project.json thất bại.
     */
    private void saveReleases() throws IOException {
        ProjectConfig config = projectService.getCurrentProjectConfig();
        if (config == null) {
            throw new IOException("Không có cấu hình dự án nào đang tải để lưu.");
        }

        ArrayList<Map<String, String>> dataList = new ArrayList<>();
        releasesList.forEach(model -> dataList.add(model.toMap()));

        config.setReleases(dataList);
        projectService.saveCurrentProjectConfig();
        projectStateService.setStatusMessage("Đã cập nhật danh sách Release.");
        logger.info("Đã lưu danh sách Release vào project.json");
    }

    /**
     * Thêm hoặc Cập nhật một Release.
     *
     * @param model Model (View) chứa dữ liệu từ Form
     * @return Đối tượng ReleaseModel (hoặc null) đã được tải lại
     * @throws IOException Nếu lưu thất bại
     */
    public ReleaseModel saveOrUpdateRelease(ReleaseModel model) throws IOException {
        if (model == null || model.getId().isEmpty() || model.getName().isEmpty()) {
            throw new IOException("ID và Tên Release không được rỗng.");
        }

        /**
         * Kiểm tra trùng lặp ID (UC-CFG-02, 1.0.E1)
         */
        Optional<ReleaseModel> existing = releasesList.stream()
                .filter(r -> r.getId().equalsIgnoreCase(model.getId()) && r != model)
                .findFirst();

        if (existing.isPresent()) {
            throw new IOException("ID Release '" + model.getId() + "' đã tồn tại.");
        }

        /**
         * Nếu là model mới (chưa có trong danh sách), thêm vào
         */
        if (!releasesList.contains(model)) {
            releasesList.add(model);
        }

        saveReleases();

        /**
         * Tải lại (load) danh sách sau khi lưu.
         */
        loadReleases();

        /**
         * [SỬA LỖI] Tìm và trả về
         * đối tượng tương đương trong danh sách
         * vừa được tải lại (reloaded list).
         */
        return releasesList.stream()
                .filter(r -> r.getId().equals(model.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Xóa một Release (UC-CFG-02, 1.0.A2).
     *
     * @param model Model (View) cần xóa
     * @throws IOException Nếu không thể xóa (vi phạm toàn vẹn)
     */
    public void deleteRelease(ReleaseModel model) throws IOException {
        if (model == null) return;

        /**
         * Kiểm tra toàn vẹn (UC-CFG-02, 1.0.A2, Bước 3.2)
         */
        if (indexService.hasBacklinks(model.getId())) {
            logger.warn("Ngăn chặn xóa Release {}: Đang có liên kết ngược.", model.getId());
            /**
             * Gọi model.getName()
             */
            throw new IOException("Không thể xóa '" + model.getName() + "'. " +
                    "Đang có các artifact (ví dụ: Use Case) được gán cho Release này.");
        }

        releasesList.remove(model);
        saveReleases();
    }


    public ObservableList<ReleaseModel> getReleasesList() {
        return releasesList;
    }

    public ObjectProperty<ReleaseModel> selectedReleaseProperty() {
        return selectedRelease;
    }
}