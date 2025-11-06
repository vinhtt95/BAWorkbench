package com.rms.app.viewmodel;

import com.fasterxml.jackson.databind.ObjectMapper; // Thêm import
import com.fasterxml.jackson.core.type.TypeReference; // Thêm import
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.FlowStep;
import com.rms.app.service.IArtifactRepository;
import com.rms.app.service.IProjectStateService;
import javafx.animation.PauseTransition;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; // Thêm import
import java.util.Map;

/**
 * "Brain" cho ArtifactView.
 * Quản lý trạng thái, logic UI, và binding cho một Artifact.
 * Xử lý logic Auto-save.
 */
public class ArtifactViewModel {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactViewModel.class);

    private final IArtifactRepository artifactRepository;
    private final IProjectStateService projectStateService;
    private final ObjectMapper objectMapper; // Thêm ObjectMapper

    /**
     * Model (dữ liệu)
     */
    private final Artifact artifact;

    /**
     * Properties cho các trường (fields) cố định
     */
    private final StringProperty id;
    private final StringProperty name;

    /**
     * Map này lưu các Property (StringProperty, ObjectProperty, v.v.)
     * cho các trường động (từ Form Builder)
     */
    private final Map<String, Property<?>> dynamicFields = new HashMap<>();

    /**
     * Timer cho logic Auto-save (Ngày 12)
     */
    private final PauseTransition autoSaveTimer;

    @Inject
    public ArtifactViewModel(IArtifactRepository artifactRepository, IProjectStateService projectStateService) {
        this.artifactRepository = artifactRepository;
        this.projectStateService = projectStateService;
        this.objectMapper = new ObjectMapper(); // Khởi tạo ObjectMapper

        /**
         * Khởi tạo một Artifact mới (cho UC-DEV-01 Tạo mới)
         */
        this.artifact = new Artifact();
        this.artifact.setFields(new HashMap<>());

        /**
         * Khởi tạo Properties
         */
        this.id = new SimpleStringProperty("Đang chờ lưu...");
        this.name = new SimpleStringProperty();

        /**
         * Dùng PauseTransition để kích hoạt save 2s sau khi dừng gõ
         */
        this.autoSaveTimer = new PauseTransition(Duration.seconds(2));
        this.autoSaveTimer.setOnFinished(event -> saveArtifact());

        /**
         * Lắng nghe thay đổi trên các property để kích hoạt auto-save
         */
        this.name.addListener((obs, oldV, newV) -> triggerAutoSave());
    }

    /**
     * Hàm này được gọi bởi RenderService để tạo các binding.
     * Nó khởi tạo các property động cho các trường (field) kiểu String.
     *
     * @param fieldName Tên của trường (field)
     * @return Property (javafx) tương ứng
     */
    public Property<?> getFieldProperty(String fieldName) {
        return dynamicFields.computeIfAbsent(fieldName, key -> {
            /**
             * Lấy giá trị ban đầu từ model
             */
            Object initialValue = artifact.getFields().get(key);

            StringProperty property = new SimpleStringProperty((String) initialValue);

            /**
             * Lắng nghe thay đổi để kích hoạt auto-save
             */
            property.addListener((obs, oldV, newV) -> triggerAutoSave());
            return property;
        });
    }

    /**
     * [SỬA LỖI NGÀY 15/16]
     * Hàm này được gọi bởi RenderService để tạo binding cho Flow Builder.
     * Nó sẽ kiểm tra dữ liệu đã lưu trong Artifact, chuyển đổi (convert)
     * và nạp (load) chúng vào ObservableList.
     *
     * @param fieldName Tên của trường (field) (ví dụ: "MainFlow")
     * @return Một ObservableList chứa các FlowStep
     */
    public ObservableList<FlowStep> getFlowStepProperty(String fieldName) {
        Property<?> existing = dynamicFields.get(fieldName);
        if (existing != null) {
            return (ObservableList<FlowStep>) existing.getValue();
        }

        /**
         * [SỬA LỖI] Bắt đầu logic nạp (load) dữ liệu
         */
        List<FlowStep> initialSteps = new ArrayList<>();
        Object rawData = artifact.getFields().get(fieldName);

        if (rawData instanceof List) {
            try {
                /**
                 * Khi Jackson deserialize file JSON, nó không biết kiểu FlowStep,
                 * nên nó lưu dưới dạng List<Map<String, Object>>.
                 * Chúng ta cần dùng ObjectMapper để convert lại.
                 */
                initialSteps = objectMapper.convertValue(
                        rawData,
                        new TypeReference<List<FlowStep>>() {}
                );
            } catch (Exception e) {
                logger.error("Không thể convert FlowStep data khi load", e);
                projectStateService.setStatusMessage("Lỗi: Không thể nạp Flow Builder.");
            }
        }

        /**
         * Tạo mới nếu chưa có, hoặc nạp (load) từ dữ liệu đã convert
         */
        ObservableList<FlowStep> list = FXCollections.observableArrayList(initialSteps);
        SimpleListProperty<FlowStep> property = new SimpleListProperty<>(list);

        /**
         * Kích hoạt auto-save khi list thay đổi
         */
        list.addListener((javafx.collections.ListChangeListener.Change<? extends FlowStep> c) -> triggerAutoSave());

        dynamicFields.put(fieldName, property);
        return list;
    }

    /**
     * Kích hoạt (hoặc reset) timer auto-save.
     */
    private void triggerAutoSave() {
        autoSaveTimer.playFromStart();
    }

    /**
     * Logic nghiệp vụ để lưu Artifact xuống đĩa (qua Repository).
     */
    private void saveArtifact() {
        logger.debug("Kích hoạt Auto-save...");
        try {
            if (artifact.getId() == null) {
                String newId = "NEW-" + System.currentTimeMillis();
                artifact.setId(newId);
                artifact.setArtifactType("TEMP_TYPE");
                id.set(newId);
            }

            artifact.setName(name.get());

            /**
             * Đảm bảo ViewModel cập nhật đúng cấu trúc Flow
             */
            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                if (entry.getValue() instanceof SimpleListProperty) {
                    /**
                     * Nếu là FlowStep list
                     */
                    artifact.getFields().put(entry.getKey(), new ArrayList<>(((SimpleListProperty<?>) entry.getValue()).get()));
                } else {
                    /**
                     * Nếu là StringProperty
                     */
                    artifact.getFields().put(entry.getKey(), entry.getValue().getValue());
                }
            }

            artifactRepository.save(artifact);
            projectStateService.setStatusMessage("Đã lưu " + artifact.getId());
        } catch (IOException e) {
            logger.error("Lỗi Auto-save", e);
            projectStateService.setStatusMessage("Lỗi Auto-save: " + e.getMessage());
        }
    }

    /**
     * Getter cho ID (dùng bởi RenderService).
     * @return ID của artifact
     */
    public String getId() { return id.get(); }

    /**
     * Getter cho Name (dùng bởi RenderService).
     * @return Property Tên (Name)
     */
    public StringProperty nameProperty() { return name; }
}