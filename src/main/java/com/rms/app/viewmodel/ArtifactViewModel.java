package com.rms.app.viewmodel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.FlowStep;
import com.rms.app.model.ProjectConfig; // [THÊM MỚI] Import
import com.rms.app.service.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform; // [THÊM MỚI] Import
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task; // [THÊM MỚI] Import
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
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
    private final IApiService apiService; // [THÊM MỚI]
    private final IProjectService projectService; // [THÊM MỚI]

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
                             IDiagramRenderService diagramRenderService,
                             IApiService apiService,
                             IProjectService projectService) {
        this.artifactRepository = artifactRepository;
        this.projectStateService = projectStateService;
        this.diagramRenderService = diagramRenderService;
        this.apiService = apiService; // [THÊM MỚI]
        this.projectService = projectService; // [THÊM MỚI]
        this.objectMapper = new ObjectMapper();

        this.objectMapper.findAndRegisterModules();

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
        return getStringProperty(fieldName);
    }

    /**
     * Cung cấp StringProperty (dùng cho TextField, TextArea, Dropdown, Linker)
     */
    public StringProperty getStringProperty(String fieldName) {
        return (StringProperty) dynamicFields.computeIfAbsent(fieldName, key -> {
            Object initialValue = artifact.getFields().get(key);
            StringProperty property = new SimpleStringProperty((String) initialValue);
            property.addListener((obs, oldV, newV) -> triggerAutoSave());
            return property;
        });
    }

    /**
     * Cung cấp ObjectProperty<LocalDate> (dùng cho DatePicker)
     */
    public ObjectProperty<LocalDate> getLocalDateProperty(String fieldName) {
        return (ObjectProperty<LocalDate>) dynamicFields.computeIfAbsent(fieldName, key -> {
            Object initialValueRaw = artifact.getFields().get(key);
            LocalDate initialValue = null;

            if (initialValueRaw instanceof String) {
                try {
                    initialValue = LocalDate.parse((String) initialValueRaw);
                } catch (Exception e) {
                    logger.warn("Giá trị ngày không hợp lệ trong JSON: {}", initialValueRaw);
                }
            } else if (initialValueRaw != null) {
                try {
                    initialValue = objectMapper.convertValue(initialValueRaw, LocalDate.class);
                } catch (Exception e) {
                    logger.error("Không thể convert giá trị ngày: {}", initialValueRaw, e);
                }
            }

            ObjectProperty<LocalDate> property = new SimpleObjectProperty<>(initialValue);
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
        list.addListener((javafx.collections.ListChangeListener.Change<? extends FlowStep> c) -> {
            while (c.next()) {
            }
            triggerAutoSave();
        });

        SimpleListProperty<FlowStep> property = new SimpleListProperty<>(list);
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
                String newId = this.templatePrefix + "-" + (System.currentTimeMillis() % 100000);
                artifact.setId(newId);
                artifact.setArtifactType(this.templatePrefix);
                id.set(newId);
            }

            artifact.setName(name.get());

            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                if (entry.getValue() instanceof SimpleListProperty) {
                    artifact.getFields().put(entry.getKey(), new ArrayList<>(((SimpleListProperty<?>) entry.getValue()).get()));
                } else if (entry.getValue() instanceof SimpleObjectProperty) {
                    Object value = entry.getValue().getValue();
                    artifact.getFields().put(entry.getKey(), (value != null) ? value.toString() : null);
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
     * Logic nghiệp vụ để sinh sơ đồ (UC-MOD-01)
     *
     * @return Một Image (JavaFX) của sơ đồ
     */
    public Image generateDiagram() {
        try {
            List<FlowStep> flowSteps = new ArrayList<>();

            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                if (entry.getValue() instanceof SimpleListProperty) {
                    flowSteps = (List<FlowStep>) new ArrayList<>(((SimpleListProperty<?>) entry.getValue()).get());
                    break;
                }
            }

            if (flowSteps.isEmpty()) {
                logger.warn("Không tìm thấy dữ liệu Flow (FlowStep) trong artifact {}", artifact.getId());
                return null;
            }

            String plantUmlCode = diagramRenderService.generatePlantUmlCode(flowSteps);
            BufferedImage awtImage = diagramRenderService.render(plantUmlCode);

            return SwingFXUtils.toFXImage(awtImage, null);

        } catch (Exception e) {
            logger.error("Không thể sinh sơ đồ cho {}: {}", artifact.getId(), e.getMessage());
            projectStateService.setStatusMessage("Lỗi render sơ đồ: " + e.getMessage());
            return null;
        }
    }

    /**
     * [THÊM MỚI] Logic nghiệp vụ cho "Gemini: Đề xuất" (UC-DEV-03).
     */
    public void generateFlowFromGemini() {
        /**
         * 1. Lấy (Get) Tên và Mô tả (Description)
         */
        String artifactName = name.get();
        String artifactDescription = getStringProperty("Description").get();
        if (artifactDescription == null) {
            artifactDescription = getStringProperty("Mô tả").get(); // Thử tên tiếng Việt
        }

        if (artifactName == null || artifactName.isEmpty() || artifactDescription == null || artifactDescription.isEmpty()) {
            projectStateService.setStatusMessage("Lỗi: Vui lòng nhập Tên (Name) và Mô tả (Description) trước.");
            return;
        }

        /**
         * 2. Lấy (Get) API Key
         */
        ProjectConfig config = projectService.getCurrentProjectConfig();
        if (config == null || config.getGeminiApiKey() == null || config.getGeminiApiKey().isEmpty()) {
            projectStateService.setStatusMessage("Lỗi: Gemini API Key chưa được cấu hình (Settings > API Keys).");
            return;
        }
        String apiKey = config.getGeminiApiKey();

        /**
         * 3. Lấy (Get) danh sách (list) Flow để cập nhật
         */
        ObservableList<FlowStep> flowStepList = null;
        for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
            if (entry.getValue() instanceof SimpleListProperty) {
                flowStepList = (ObservableList<FlowStep>) entry.getValue();
                break;
            }
        }

        if (flowStepList == null) {
            projectStateService.setStatusMessage("Lỗi: Không tìm thấy trường 'Flow Builder' trong template này.");
            return;
        }
        final ObservableList<FlowStep> targetList = flowStepList;

        /**
         * 4. Chạy Tác vụ (Task) trên luồng nền (background thread)
         */
        String finalArtifactDescription = artifactDescription;
        Task<List<FlowStep>> geminiTask = new Task<>() {
            @Override
            protected List<FlowStep> call() throws Exception {
                return apiService.generateFlowFromGemini(artifactName, finalArtifactDescription, apiKey);
            }

            @Override
            protected void succeeded() {
                List<FlowStep> suggestedSteps = getValue();
                /**
                 * 5. Cập nhật UI trên luồng FX
                 */
                Platform.runLater(() -> {
                    targetList.clear();
                    targetList.addAll(suggestedSteps);
                    projectStateService.setStatusMessage(String.format("Gemini đã đề xuất %d bước.", suggestedSteps.size()));
                });
            }

            @Override
            protected void failed() {
                logger.error("Tác vụ (Task) Gemini thất bại", getException());
                Platform.runLater(() -> projectStateService.setStatusMessage("Lỗi Gemini: " + getException().getMessage()));
            }
        };

        projectStateService.setStatusMessage(String.format("Đang gửi '%s' đến Gemini...", artifactName));
        new Thread(geminiTask).start();
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