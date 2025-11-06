package com.rms.app.viewmodel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate; // Thêm import
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
import java.util.List;
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
    private final ObjectMapper objectMapper;

    /**
     * Model (dữ liệu)
     */
    private Artifact artifact;

    /**
     * [SỬA LỖI] Lưu prefix (ví dụ: "UC") của template
     */
    private String templatePrefix;

    /**
     * Properties cho các trường (fields) cố định
     */
    private final StringProperty id;
    private final StringProperty name;

    /**
     * Map này lưu các Property (StringProperty, ObjectProperty, v.v.)
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
        this.objectMapper = new ObjectMapper();

        this.id = new SimpleStringProperty("Đang chờ lưu...");
        this.name = new SimpleStringProperty();

        this.autoSaveTimer = new PauseTransition(Duration.seconds(2));
        this.autoSaveTimer.setOnFinished(event -> saveArtifact());

        this.name.addListener((obs, oldV, newV) -> triggerAutoSave());
    }

    /**
     * [SỬA LỖI] Nạp dữ liệu (nếu có) VÀ template (luôn có) vào ViewModel.
     * Được gọi bởi ArtifactView.initialize().
     *
     * @param template       Template (form) của artifact
     * @param loadedArtifact Artifact đã load (hoặc null nếu tạo mới)
     */
    public void initializeData(ArtifactTemplate template, Artifact loadedArtifact) {
        if (template == null) {
            logger.error("ViewModel không thể khởi tạo vì template là null");
            return;
        }

        this.templatePrefix = template.getPrefixId(); // Lưu prefix

        if (loadedArtifact != null) {
            /**
             * Nếu là Mở file: Nạp dữ liệu từ artifact đã load
             */
            this.artifact = loadedArtifact;
            this.id.set(artifact.getId());
            this.name.set(artifact.getName());

            /**
             * Tái tạo (pre-populate) dynamicFields
             */
            if (artifact.getFields() != null) {
                for (Map.Entry<String, Object> entry : artifact.getFields().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof List) {
                        getFlowStepProperty(key); // Gọi để tạo và nạp list
                    } else {
                        getFieldProperty(key); // Gọi để tạo và nạp string
                    }
                }
            }
        } else {
            /**
             * Nếu là Tạo mới: Khởi tạo artifact rỗng
             */
            this.artifact = new Artifact();
            this.artifact.setFields(new HashMap<>());
            this.artifact.setArtifactType(this.templatePrefix); // Gán prefix ngay
        }
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
            Object initialValue = artifact.getFields().get(key);
            StringProperty property = new SimpleStringProperty((String) initialValue);
            property.addListener((obs, oldV, newV) -> triggerAutoSave());
            return property;
        });
    }

    /**
     * Hàm này được gọi bởi RenderService để tạo binding cho Flow Builder.
     *
     * @param fieldName Tên của trường (field) (ví dụ: "MainFlow")
     * @return Một ObservableList chứa các FlowStep
     */
    public ObservableList<FlowStep> getFlowStepProperty(String fieldName) {
        Property<?> existing = dynamicFields.get(fieldName);

        if (existing instanceof SimpleListProperty) {
            return (ObservableList<FlowStep>) existing.getValue();
        }

        List<FlowStep> initialSteps = new ArrayList<>();
        Object rawData = artifact.getFields().get(fieldName);

        if (rawData instanceof List) {
            try {
                initialSteps = objectMapper.convertValue(
                        rawData,
                        new TypeReference<List<FlowStep>>() {}
                );
            } catch (Exception e) {
                logger.error("Không thể convert FlowStep data khi load", e);
            }
        }

        ObservableList<FlowStep> list = FXCollections.observableArrayList(initialSteps);
        SimpleListProperty<FlowStep> property = new SimpleListProperty<>(list);
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
                /**
                 * Nếu là lần lưu đầu tiên (Tạo mới)
                 */
                String newId = this.templatePrefix + "-" + System.currentTimeMillis();
                artifact.setId(newId);
                artifact.setArtifactType(this.templatePrefix); // [SỬA LỖI]
                id.set(newId);
            }

            artifact.setName(name.get());

            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                if (entry.getValue() instanceof SimpleListProperty) {
                    artifact.getFields().put(entry.getKey(), new ArrayList<>(((SimpleListProperty<?>) entry.getValue()).get()));
                } else {
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