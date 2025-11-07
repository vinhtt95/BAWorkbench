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
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

        // [SỬA LỖI NGÀY 27] Hỗ trợ ObjectMapper cho Java 8 Time (LocalDate)
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

            // [SỬA LỖI NGÀY 27] Không bind ở đây,
            // để getFieldProperty/getFlowStepProperty tự động nạp
            // khi RenderService gọi chúng.

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
        // [SỬA LỖI NGÀY 27] Đổi tên thành "getStringProperty"
        // để rõ ràng nó chỉ xử lý String
        return getStringProperty(fieldName);
    }

    /**
     * [THÊM MỚI NGÀY 27] Cung cấp StringProperty (dùng cho TextField, TextArea, Dropdown, Linker)
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
     * [THÊM MỚI NGÀY 27] Cung cấp ObjectProperty<LocalDate> (dùng cho DatePicker)
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
                    // Thử convert (ví dụ: nếu Jackson đọc nó thành mảng [2025, 11, 7])
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
        // [SỬA LỖI NGÀY 27] Cần lắng nghe sự thay đổi sâu (deep change)
        list.addListener((javafx.collections.ListChangeListener.Change<? extends FlowStep> c) -> {
            while (c.next()) {
                // Chỉ cần biết có thay đổi, không cần biết chi tiết
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
                // [SỬA LỖI NGÀY 27] Cần ID ngẫu nhiên hơn
                String newId = this.templatePrefix + "-" + (System.currentTimeMillis() % 100000);
                artifact.setId(newId);
                artifact.setArtifactType(this.templatePrefix);
                id.set(newId);
            }

            artifact.setName(name.get());

            // [SỬA LỖI NGÀY 27] Cần lấy giá trị (value) đúng từ các loại Property
            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                if (entry.getValue() instanceof SimpleListProperty) {
                    // Xử lý FlowStep
                    artifact.getFields().put(entry.getKey(), new ArrayList<>(((SimpleListProperty<?>) entry.getValue()).get()));
                } else if (entry.getValue() instanceof SimpleObjectProperty) {
                    // Xử lý DatePicker (LocalDate) hoặc các Object khác
                    Object value = entry.getValue().getValue();
                    // Lưu Date thành String ISO (ví dụ: "2025-11-07")
                    artifact.getFields().put(entry.getKey(), (value != null) ? value.toString() : null);
                } else {
                    // Xử lý StringProperty (TextField, Dropdown, ...)
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
     * @return Một Image (JavaFX) của sơ đồ
     */
    public Image generateDiagram() {
        try {
            List<FlowStep> flowSteps = new ArrayList<>();

            // [SỬA LỖI NGÀY 27] Lấy dữ liệu Flow từ dynamicFields (đã binding)
            // thay vì từ artifact.getFields() (có thể đã cũ)
            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                if (entry.getValue() instanceof SimpleListProperty) {
                    // Chỉ lấy Flow đầu tiên tìm thấy
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