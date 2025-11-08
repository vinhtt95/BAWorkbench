package com.rms.app.viewmodel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.model.FlowStep;
import com.rms.app.model.ProjectConfig;
import com.rms.app.service.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Brain" cho ArtifactView.
 * [CẬP NHẬT] Sửa lỗi mất folderId khi lưu,
 * và thêm logic "Save on Close" (Lưu khi Đóng tab).
 */
public class ArtifactViewModel {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactViewModel.class);

    private final IArtifactRepository artifactRepository;
    private final IProjectStateService projectStateService;
    private final ObjectMapper objectMapper;
    private final IDiagramRenderService diagramRenderService;
    private final IApiService apiService;
    private final IProjectService projectService;

    /**
     * Model (dữ liệu)
     */
    private Artifact artifact;
    private ArtifactTemplate template;

    /**
     * [SỬA LỖI] Các trường (field) này
     * giờ đây được dùng
     * để lưu trữ vị trí của artifact.
     */
    private String parentRelativePath;
    private String parentFolderId;

    /**
     * Properties cho các trường (fields) cố định
     */
    private final StringProperty id;
    private final StringProperty name;

    public final Map<String, Property<?>> dynamicFields = new HashMap<>();
    private final PauseTransition autoSaveTimer;

    private boolean isInitialized = false;
    private Tab myTab;


    @Inject
    public ArtifactViewModel(IArtifactRepository artifactRepository,
                             IProjectStateService projectStateService,
                             IDiagramRenderService diagramRenderService,
                             IApiService apiService,
                             IProjectService projectService) {
        this.artifactRepository = artifactRepository;
        this.projectStateService = projectStateService;
        this.diagramRenderService = diagramRenderService;
        this.apiService = apiService;
        this.projectService = projectService;
        this.objectMapper = new ObjectMapper();

        this.objectMapper.findAndRegisterModules();

        this.id = new SimpleStringProperty("Đang chờ lưu...");
        this.name = new SimpleStringProperty();

        this.autoSaveTimer = new PauseTransition(Duration.seconds(2));
        this.autoSaveTimer.setOnFinished(event -> saveArtifact());

        this.name.addListener((obs, oldV, newV) -> triggerAutoSave());
    }

    /**
     * Cung cấp cờ (flag)
     * cho ArtifactView.
     */
    public boolean isNotInitialized() {
        return !this.isInitialized;
    }

    /**
     * [TÁI CẤU TRÚC]
     * Hàm này giờ đây nhận Tab (chứa UserData)
     * để phân biệt Mới (New) và Cũ (Existing).
     *
     * @param tab         Tab chứa UserData
     */
    public void initializeData(Tab tab) {
        this.isInitialized = true;
        this.myTab = tab;
        Object userData = tab.getUserData();

        if (userData instanceof Map) {
            /**
             * TRƯỜNG HỢP 1: TẠO ARTIFACT MỚI
             */
            Map<String, Object> creationData = (Map<String, Object>) userData;
            this.template = (ArtifactTemplate) creationData.get("template");
            this.parentRelativePath = (String) creationData.get("parentRelativePath");
            this.parentFolderId = (String) creationData.get("parentFolderId");

            this.artifact = new Artifact();
            this.artifact.setFields(new HashMap<>());
            this.artifact.setArtifactType(template.getPrefixId());
            this.artifact.setTemplateId(template.getTemplateId());

            tab.setText("New " + template.getPrefixId());

            /**
             * [SỬA LỖI 2] Gắn (Attach) listener (trình lắng nghe)
             * Lưu-khi-Đóng (Save-on-Close) cho tab MỚI.
             */
            this.myTab.setOnCloseRequest(event -> {
                logger.debug("Đóng tab MỚI. Đang kích hoạt lưu (save)...");
                autoSaveTimer.stop();
                saveArtifact();
            });

        } else {
            logger.error("Không thể khởi tạo ArtifactViewModel: UserData không phải là Map.");
        }
    }

    /**
     * Hàm này giờ chỉ được gọi khi MỞ (OPEN) file.
     */
    public void initializeData(ArtifactTemplate template, Artifact loadedArtifact, Tab tab) {
        if (this.isInitialized) return;
        this.isInitialized = true;
        this.myTab = tab;

        if (loadedArtifact != null) {
            // --- MỞ ARTIFACT ĐÃ TỒN TẠI ---
            this.artifact = loadedArtifact;
            this.template = template;
            this.id.set(artifact.getId());
            this.name.set(artifact.getName());

            /**
             * [SỬA LỖI 1] Phải lưu lại folderId
             * và đường dẫn (path) của artifact
             * đã tải (load),
             * nếu không, saveArtifact() sẽ set chúng thành null.
             */
            this.parentFolderId = loadedArtifact.getFolderId();
            String loadedPath = loadedArtifact.getRelativePath();
            if (loadedPath != null && !loadedPath.isEmpty()) {
                File f = new File(loadedPath);
                String parentDir = f.getParent();
                this.parentRelativePath = (parentDir != null) ? parentDir : "";
            } else {
                this.parentRelativePath = "";
            }

            /**
             * [SỬA LỖI 2] Gắn (Attach) listener (trình lắng nghe)
             * Lưu-khi-Đóng (Save-on-Close) cho tab CŨ.
             */
            this.myTab.setOnCloseRequest(event -> {
                logger.debug("Đóng tab CŨ ({}). Đang kích hoạt lưu (save)...", loadedArtifact.getId());
                autoSaveTimer.stop();
                saveArtifact();
            });

        } else {
            // --- (Dự phòng) TẠO ARTIFACT MỚI ---
            this.template = template;
            this.artifact = new Artifact();
            this.artifact.setFields(new HashMap<>());
            this.artifact.setArtifactType(template.getPrefixId());
            this.artifact.setTemplateId(template.getTemplateId());
            logger.warn("ArtifactViewModel được khởi tạo (MỚI) mà không có dữ liệu Tab. RelativePath sẽ là null.");

            /**
             * [SỬA LỖI 2] Gắn (Attach) listener (trình lắng nghe)
             * Lưu-khi-Đóng (Save-on-Close) cho tab MỚI
             * (trường hợp dự phòng).
             */
            this.myTab.setOnCloseRequest(event -> {
                logger.debug("Đóng tab MỚI (Dự phòng). Đang kích hoạt lưu (save)...");
                autoSaveTimer.stop();
                saveArtifact();
            });
        }
    }


    /**
     * Hàm này được gọi bởi RenderService để tạo các binding.
     *
     * @param fieldName Tên của trường (field)
     * @return Property (javafx) tương ứng
     */
    public Property<?> getFieldProperty(String fieldName) {
        if ("ID".equalsIgnoreCase(fieldName)) {
            return this.id;
        }
        if ("Name".equalsIgnoreCase(fieldName)) {
            return this.name;
        }

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
                /**
                 * LƯU LẦN ĐẦU (Tạo mới)
                 */
                String newId = template.getPrefixId() + "-" + (System.currentTimeMillis() % 100000);
                artifact.setId(newId);
                id.set(newId);

                if (this.parentRelativePath == null) {
                    logger.error("parentRelativePath là null khi lưu artifact mới. Auto-save thất bại.");
                    throw new IOException("Không thể lưu artifact: relativePath là null hoặc rỗng.");
                }

                String newPath = Path.of(this.parentRelativePath, newId + ".json").toString();
                artifact.setRelativePath(newPath);
                artifact.setFolderId(this.parentFolderId);
            } else {
                /**
                 * [SỬA LỖI 1] LƯU FILE ĐÃ CÓ
                 * Đảm bảo folderId (đã được nạp
                 * trong initializeData)
                 * được gán lại vào artifact trước khi lưu.
                 */
                artifact.setFolderId(this.parentFolderId);
            }

            artifact.setName(name.get());

            for (Map.Entry<String, Property<?>> entry : dynamicFields.entrySet()) {
                if (entry.getValue() instanceof SimpleListProperty) {
                    artifact.getFields().put(entry.getKey(), new ArrayList<>(((SimpleListProperty<?>) entry.getValue()).get()));
                } else if (entry.getValue() instanceof SimpleObjectProperty) {
                    Object value = entry.getValue().getValue();
                    artifact.getFields().put(entry.getKey(), (value != null) ? value.toString() : null);
                } else {
                    if (!entry.getKey().equalsIgnoreCase("ID") && !entry.getKey().equalsIgnoreCase("Name")) {
                        artifact.getFields().put(entry.getKey(), entry.getValue().getValue());
                    }
                }
            }

            artifactRepository.save(artifact);

            if (myTab != null && myTab.getUserData() instanceof Map) {
                myTab.setText(artifact.getId());
                myTab.setUserData(artifact.getId());
            }

            projectStateService.setStatusMessage("Đã lưu " + artifact.getId());
        } catch (IOException e) {
            logger.error("Lỗi Auto-save", e);
            projectStateService.setStatusMessage("Lỗi Auto-save: " + e.getMessage());
        }
    }

    /**
     * Helper (hàm phụ)
     * để tìm (find) Tab (JavaFX)
     * mà ViewModel này đang quản lý.
     */
    private Tab findMyTab() {
        if (myTab != null) {
            return myTab;
        }

        try {
            Property<?> nameProp = dynamicFields.get("Name");
            if (nameProp != null && nameProp.getValue() instanceof TextField) {
                TextField nameFieldNode = (TextField) nameProp.getValue();
                if (nameFieldNode.getScene() != null) {
                    TabPane tabPane = (TabPane) nameFieldNode.getScene().lookup("#mainTabPane");
                    if (tabPane != null) {
                        for (Tab tab : tabPane.getTabs()) {
                            if (tab.getContent() != null && tab.getContent().equals(nameFieldNode.getParent().getParent().getParent())) {
                                this.myTab = tab;
                                return tab;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Không thể tìm thấy Tab (findMyTab) để cập nhật UserData.");
        }
        return null;
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
                if (artifact != null && artifact.getId() != null) {
                    logger.warn("Không tìm thấy dữ liệu Flow (FlowStep) trong artifact {}", artifact.getId());
                }
                return null;
            }

            String plantUmlCode = diagramRenderService.generatePlantUmlCode(flowSteps);
            BufferedImage awtImage = diagramRenderService.render(plantUmlCode);

            return SwingFXUtils.toFXImage(awtImage, null);

        } catch (Exception e) {
            logger.error("Không thể sinh sơ đồ: {}", e.getMessage());
            projectStateService.setStatusMessage("Lỗi render sơ đồ: " + e.getMessage());
            return null;
        }
    }

    /**
     * Logic nghiệp vụ cho "Gemini: Đề xuất" (UC-DEV-03).
     */
    public void generateFlowFromGemini() {
        String artifactName = name.get();
        String artifactDescription = getStringProperty("Description").get();
        if (artifactDescription == null) {
            artifactDescription = getStringProperty("Mô tả").get();
        }

        if (artifactName == null || artifactName.isEmpty() || artifactDescription == null || artifactDescription.isEmpty()) {
            projectStateService.setStatusMessage("Lỗi: Vui lòng nhập Tên (Name) và Mô tả (Description) trước.");
            return;
        }

        ProjectConfig config = projectService.getCurrentProjectConfig();
        if (config == null || config.getGeminiApiKey() == null || config.getGeminiApiKey().isEmpty()) {
            projectStateService.setStatusMessage("Lỗi: Gemini API Key chưa được cấu hình (Settings > API Keys).");
            return;
        }
        String apiKey = config.getGeminiApiKey();

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

        String finalArtifactDescription = artifactDescription;
        Task<List<FlowStep>> geminiTask = new Task<>() {
            @Override
            protected List<FlowStep> call() throws Exception {
                return apiService.generateFlowFromGemini(artifactName, finalArtifactDescription, apiKey);
            }

            @Override
            protected void succeeded() {
                List<FlowStep> suggestedSteps = getValue();
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

    /**
     * Getter cho Template
     * (dùng bởi RenderService)
     */
    public ArtifactTemplate getTemplate() {
        return this.template;
    }
}