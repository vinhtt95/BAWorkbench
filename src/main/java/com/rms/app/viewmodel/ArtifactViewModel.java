package com.rms.app.viewmodel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.FlowStep;
import com.rms.app.service.IArtifactRepository;
import com.rms.app.service.IDiagramRenderService;
import com.rms.app.service.IProjectStateService;
import javafx.animation.PauseTransition;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.image.BufferedImage;
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
    private final IDiagramRenderService diagramRenderService;

    /**
     * Model (dữ liệu)
     */
    private Artifact artifact;

    /**
     * Lưu prefix (ví dụ: "UC") của template
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
    public final Map<String, Property<?>> dynamicFields = new HashMap<>();

    /**
     * Timer cho logic Auto-save (Ngày 12)
     */
    private final PauseTransition autoSaveTimer;

    @Inject
    public ArtifactViewModel(IArtifactRepository artifactRepository,
                             IProjectStateService projectStateService,
                             IDiagramRenderService diagramRenderService) {
        this.artifactRepository = artifactRepository;
        this.projectStateService = projectStateService;
        this.diagramRenderService = diagramRenderService;
        this.objectMapper = new ObjectMapper();

        this.id = new SimpleStringProperty("Đang chờ lưu...");
        this.name = new SimpleStringProperty();

        this.autoSaveTimer = new PauseTransition(Duration.seconds(2));
        this.autoSaveTimer.setOnFinished(event -> saveArtifact());

        this.name.addListener((obs, oldV, newV) -> triggerAutoSave());
    }

    /**
     * Nạp dữ liệu (nếu có) VÀ template (luôn có) vào ViewModel.
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

        this.templatePrefix = template.getPrefixId();

        if (loadedArtifact != null) {
            this.artifact = loadedArtifact;
            this.id.set(artifact.getId());
            this.name.set(artifact.getName());

            if (artifact.getFields() != null) {
                for (Map.Entry<String, Object> entry : artifact.getFields().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof List) {
                        getFlowStepProperty(key);
                    } else {
                        getFieldProperty(key);
                    }
                }
            }
        } else {
            this.artifact = new Artifact();
            this.artifact.setFields(new HashMap<>());
            this.artifact.setArtifactType(this.templatePrefix);
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
                String newId = this.templatePrefix + "-" + System.currentTimeMillis();
                artifact.setId(newId);
                artifact.setArtifactType(this.templatePrefix);
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
     * [THÊM MỚI NGÀY 26]
     * Logic nghiệp vụ để sinh sơ đồ (UC-MOD-01)
     *
     * @return Một Image (JavaFX) của sơ đồ, hoặc null nếu lỗi
     */
    public Image generateDiagram() {
        try {
            List<FlowStep> flowSteps = new ArrayList<>();

            for (Map.Entry<String, Object> entry : artifact.getFields().entrySet()) {
                if (entry.getValue() instanceof List) {
                    flowSteps = objectMapper.convertValue(
                            entry.getValue(),
                            new TypeReference<List<FlowStep>>() {}
                    );
                    break;
                }
            }

            if (flowSteps.isEmpty()) {
                logger.warn("Không tìm thấy dữ liệu Flow (FlowStep) trong artifact {}", (artifact != null ? artifact.getId() : "new"));
                return null;
            }

            String plantUmlCode = diagramRenderService.generatePlantUmlCode(flowSteps);
            BufferedImage awtImage = diagramRenderService.render(plantUmlCode);

            return SwingFXUtils.toFXImage(awtImage, null);

        } catch (Exception e) {
            logger.error("Không thể sinh sơ đồ cho {}: {}", (artifact != null ? artifact.getId() : "new"), e.getMessage());
            projectStateService.setStatusMessage("Lỗi render sơ đồ: " + e.getMessage());
            return null;
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