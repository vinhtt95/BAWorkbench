package com.rms.app.viewmodel;

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
import java.util.Map;

// "Brain" cho ArtifactView
public class ArtifactViewModel {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactViewModel.class);

    private final IArtifactRepository artifactRepository;
    private final IProjectStateService projectStateService;

    // Model (dữ liệu)
    private final Artifact artifact;

    // Properties cho các trường (fields)
    // Các trường cố định
    private final StringProperty id;
    private final StringProperty name;

    // Các trường động (từ Form Builder)
    // Map này lưu các Property (StringProperty, ObjectProperty, v.v.)
    private final Map<String, Property<?>> dynamicFields = new HashMap<>();

    // Logic Auto-save (Ngày 12)
    private final PauseTransition autoSaveTimer;

    @Inject
    public ArtifactViewModel(IArtifactRepository artifactRepository, IProjectStateService projectStateService) {
        this.artifactRepository = artifactRepository;
        this.projectStateService = projectStateService;

        // Khởi tạo một Artifact mới (cho UC-DEV-01 Tạo mới)
        this.artifact = new Artifact();
        this.artifact.setFields(new HashMap<>());

        // Khởi tạo Properties
        this.id = new SimpleStringProperty("Đang chờ lưu...");
        this.name = new SimpleStringProperty();

        // 12.0. Dùng PauseTransition để kích hoạt save 2s sau khi dừng gõ
        this.autoSaveTimer = new PauseTransition(Duration.seconds(2));
        this.autoSaveTimer.setOnFinished(event -> saveArtifact());

        // Lắng nghe thay đổi trên các property để kích hoạt auto-save
        this.name.addListener((obs, oldV, newV) -> triggerAutoSave());
    }

    /**
     * Hàm này được gọi bởi RenderService để tạo các binding.
     * Nó khởi tạo các property động.
     */
    public Property<?> getFieldProperty(String fieldName) {
        return dynamicFields.computeIfAbsent(fieldName, key -> {
            // Lấy giá trị ban đầu từ model
            Object initialValue = artifact.getFields().get(key);

            // Tạm thời chỉ hỗ trợ StringProperty
            StringProperty property = new SimpleStringProperty((String) initialValue);

            // Lắng nghe thay đổi để kích hoạt auto-save
            property.addListener((obs, oldV, newV) -> triggerAutoSave());
            return property;
        });
    }

    public ObservableList<FlowStep> getFlowStepProperty(String fieldName) {
        Property<?> existing = dynamicFields.get(fieldName);
        if (existing != null) {
            return (ObservableList<FlowStep>) existing.getValue();
        }

        // Tạo mới nếu chưa có
        ObservableList<FlowStep> list = FXCollections.observableArrayList();
        SimpleListProperty<FlowStep> property = new SimpleListProperty<>(list);

        // Kích hoạt auto-save khi list thay đổi
        list.addListener((javafx.collections.ListChangeListener.Change<? extends FlowStep> c) -> triggerAutoSave());

        dynamicFields.put(fieldName, property);
        return list;
    }

    private void triggerAutoSave() {
        // 9.0. (Auto-save) BA dừng nhập liệu
        autoSaveTimer.playFromStart();
    }

    private void saveArtifact() {
        logger.debug("Kích hoạt Auto-save...");
        try {
            if (artifact.getId() == null) {
                // TODO: Logic tạo ID tốt hơn (dùng prefix từ template)
                String newId = "NEW-" + System.currentTimeMillis();
                artifact.setId(newId);
                artifact.setArtifactType("TEMP_TYPE"); // TODO: Lấy từ template
                id.set(newId);
            }

            artifact.setName(name.get());

            // 15.0. Đảm bảo ViewModel cập nhật đúng cấu trúc Flow
            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                if (entry.getValue() instanceof SimpleListProperty) {
                    // Nếu là FlowStep list
                    artifact.getFields().put(entry.getKey(), new ArrayList<>(((SimpleListProperty) entry.getValue()).get()));
                } else {
                    // Nếu là StringProperty
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

    // --- Getters cho RenderService/View ---
    public String getId() { return id.get(); }
    public StringProperty nameProperty() { return name; }
}