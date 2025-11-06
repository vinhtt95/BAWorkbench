package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.IArtifactRepository;
import javafx.animation.PauseTransition;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// "Brain" cho ArtifactView
public class ArtifactViewModel {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactViewModel.class);

    private final IArtifactRepository artifactRepository;
    private final MainViewModel mainViewModel;

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
    public ArtifactViewModel(IArtifactRepository artifactRepository, MainViewModel mainViewModel) {
        this.artifactRepository = artifactRepository;
        this.mainViewModel = mainViewModel;

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

    private void triggerAutoSave() {
        // 9.0. (Auto-save) BA dừng nhập liệu
        autoSaveTimer.playFromStart();
    }

    private void saveArtifact() {
        // 10.0. Hệ thống tự động kích hoạt lưu
        logger.debug("Kích hoạt Auto-save...");

        try {
            // 1. Cập nhật ID nếu là lần lưu đầu tiên
            if (artifact.getId() == null) {
                // 11.0. Hệ thống gán một ID duy nhất
                // TODO: Logic tạo ID tốt hơn (dùng prefix từ template)
                String newId = "NEW-" + System.currentTimeMillis();
                artifact.setId(newId);
                id.set(newId); // Cập nhật UI
            }

            // 2. Đồng bộ (sync) dữ liệu từ Properties -> Model (Artifact)
            artifact.setName(name.get());
            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                artifact.getFields().put(entry.getKey(), entry.getValue().getValue());
            }

            // 3. Gọi Repository để lưu
            // 12.0. Hệ thống lưu dữ liệu Form vào file .json
            artifactRepository.save(artifact);

            mainViewModel.statusMessageProperty().set("Đã lưu " + artifact.getId());

        } catch (IOException e) {
            // 1.0.E1: Lỗi Auto-save
            logger.error("Lỗi Auto-save", e);
            mainViewModel.statusMessageProperty().set("Lỗi Auto-save: " + e.getMessage());
        }
    }

    // --- Getters cho RenderService/View ---
    public String getId() { return id.get(); }
    public StringProperty nameProperty() { return name; }
}