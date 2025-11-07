package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.ExportTemplate;
import com.rms.app.model.ExportTemplateSection;
import com.rms.app.service.IProjectStateService;
import com.rms.app.service.ISqliteIndexRepository;
import com.rms.app.service.ITemplateService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * "Brain" - Logic UI cho ExportTemplateBuilderView.
 * Tuân thủ Kế hoạch Ngày 32 (UC-CFG-03).
 */
public class ExportTemplateBuilderViewModel {

    private static final Logger logger = LoggerFactory.getLogger(ExportTemplateBuilderViewModel.class);
    private final ITemplateService templateService;
    private final IProjectStateService projectStateService;
    private final ISqliteIndexRepository indexRepository;

    /**
     * Danh sách Tên Template (Cột 1)
     */
    public final ObservableList<String> templateNames = FXCollections.observableArrayList();

    /**
     * Template POJO đang được chỉnh sửa
     */
    private ExportTemplate currentTemplate;

    /**
     * Binding cho Tên (Name) của Template (Cột 2)
     */
    public final StringProperty templateName = new SimpleStringProperty();

    /**
     * Danh sách các Chương (Section) (Cột 2)
     */
    public final ObservableList<ExportTemplateSection> currentSections = FXCollections.observableArrayList();

    /**
     * Chương (Section) POJO đang được chọn (Cột 3)
     */
    public final ObjectProperty<ExportTemplateSection> selectedSection = new SimpleObjectProperty<>(null);

    /**
     * [SỬA LỖI] Các Property (thuộc tính) cho Cột 3 (Properties Pane)
     * Thay vì View tự quản lý, ViewModel sẽ quản lý.
     */
    public final StringProperty currentSectionTitle = new SimpleStringProperty();
    public final StringProperty currentSectionType = new SimpleStringProperty();
    public final StringProperty currentSectionContent = new SimpleStringProperty();
    public final StringProperty currentQueryType = new SimpleStringProperty();
    public final StringProperty currentQueryStatus = new SimpleStringProperty();
    public final StringProperty currentDisplayFormat = new SimpleStringProperty();

    /**
     * [SỬA LỖI] Biến (field) để lưu trữ listener (trình lắng nghe)
     * Dùng để bind (thủ công)
     */
    private ChangeListener<String> titleListener;
    private ChangeListener<String> typeListener;
    private ChangeListener<String> contentListener;
    private ChangeListener<String> queryTypeListener;
    private ChangeListener<String> queryStatusListener;
    private ChangeListener<String> displayFormatListener;


    /**
     * Các tùy chọn (Option) cho ComboBox
     */
    public final ObservableList<String> sectionTypeOptions = FXCollections.observableArrayList("Static", "Dynamic");
    public final ObservableList<String> displayFormatOptions = FXCollections.observableArrayList("FullContent", "Table");
    public final ObservableList<String> artifactTypeOptions = FXCollections.observableArrayList();
    public final ObservableList<String> statusOptions = FXCollections.observableArrayList("Draft", "In Review", "Approved");

    @Inject
    public ExportTemplateBuilderViewModel(ITemplateService templateService, IProjectStateService projectStateService, ISqliteIndexRepository indexRepository) {
        this.templateService = templateService;
        this.projectStateService = projectStateService;
        this.indexRepository = indexRepository;

        loadArtifactTypeOptions();

        /**
         * [SỬA LỖI] Thêm logic binding thủ công phức tạp
         * (Tương tự như trong FormBuilderViewModel)
         */
        selectedSection.addListener((obs, oldSection, newSection) -> {
            // 1. Hủy (remove) tất cả listener cũ khỏi các Property
            unbindSectionProperties();

            if (newSection != null) {
                // 2. Cập nhật các Property TỪ POJO (newSection)
                updatePropertiesFromPojo(newSection);

                // 3. Thêm listener mới để cập nhật POJO TỪ các Property
                bindPropertiesToPojo(newSection);
            }
        });
    }

    /**
     * [SỬA LỖI] Helper (hàm phụ) để gỡ bỏ (remove) các listener cũ.
     */
    private void unbindSectionProperties() {
        if (titleListener != null) currentSectionTitle.removeListener(titleListener);
        if (typeListener != null) currentSectionType.removeListener(typeListener);
        if (contentListener != null) currentSectionContent.removeListener(contentListener);
        if (queryTypeListener != null) currentQueryType.removeListener(queryTypeListener);
        if (queryStatusListener != null) currentQueryStatus.removeListener(queryStatusListener);
        if (displayFormatListener != null) currentDisplayFormat.removeListener(displayFormatListener);
    }

    /**
     * [SỬA LỖI] Helper (hàm phụ) để nạp (load) dữ liệu từ POJO
     * vào các Property của ViewModel.
     *
     * @param pojo Section (POJO) mới được chọn
     */
    private void updatePropertiesFromPojo(ExportTemplateSection pojo) {
        currentSectionTitle.set(pojo.getTitle());
        currentSectionType.set(pojo.getType());
        currentSectionContent.set(pojo.getContent());
        currentDisplayFormat.set(pojo.getDisplayFormat());

        if (pojo.getQuery() != null) {
            currentQueryType.set(pojo.getQuery().get("artifactType"));
            currentQueryStatus.set(pojo.getQuery().get("status"));
        } else {
            pojo.setQuery(new HashMap<>()); // Đảm bảo query không bao giờ null
            currentQueryType.set(null);
            currentQueryStatus.set(null);
        }
    }

    /**
     * [SỬA LỖI] Helper (hàm phụ) để tạo và gán (assign) các listener
     * mới, cập nhật POJO khi Property thay đổi.
     *
     * @param pojo Section (POJO) mới được chọn
     */
    private void bindPropertiesToPojo(ExportTemplateSection pojo) {
        // Tạo listener mới
        titleListener = (o, oldV, newV) -> {
            pojo.setTitle(newV);
            // Hack: Buộc ListView refresh để hiển thị tên mới
            int index = currentSections.indexOf(pojo);
            if (index != -1) currentSections.set(index, pojo);
        };
        typeListener = (o, oldV, newV) -> pojo.setType(newV);
        contentListener = (o, oldV, newV) -> pojo.setContent(newV);
        displayFormatListener = (o, oldV, newV) -> pojo.setDisplayFormat(newV);
        queryTypeListener = (o, oldV, newV) -> pojo.getQuery().put("artifactType", newV);
        queryStatusListener = (o, oldV, newV) -> pojo.getQuery().put("status", newV);

        // Gán (assign) listener
        currentSectionTitle.addListener(titleListener);
        currentSectionType.addListener(typeListener);
        currentSectionContent.addListener(contentListener);
        currentQueryType.addListener(queryTypeListener);
        currentQueryStatus.addListener(queryStatusListener);
        currentDisplayFormat.addListener(displayFormatListener);
    }


    /**
     * Tải (load) các tùy chọn (option) cho Query Builder (UC-CFG-03, Bước 11.0)
     */
    private void loadArtifactTypeOptions() {
        try {
            artifactTypeOptions.clear();
            artifactTypeOptions.addAll(indexRepository.getDefinedTypes());
        } catch (SQLException e) {
            logger.error("Không thể tải Artifact Types cho Query Builder", e);
        }
    }

    /**
     * Tải danh sách tên các Template Xuất bản
     */
    public void loadTemplateNames() {
        try {
            templateNames.clear();
            templateNames.addAll(templateService.loadAllExportTemplateNames());
        } catch (IOException e) {
            logger.error("Không thể tải danh sách Export Template", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tải (load) dữ liệu của một template khi được chọn
     *
     * @param name Tên của template
     */
    public void loadTemplateForEditing(String name) {
        if (name == null) {
            currentTemplate = null;
            templateName.set("");
            currentSections.clear();
            selectedSection.set(null); // Sẽ kích hoạt listener, clear Cột 3
            return;
        }

        try {
            ExportTemplate template = templateService.loadExportTemplate(name);
            currentTemplate = template;
            templateName.set(template.getTemplateName());
            currentSections.setAll(template.getSections());
            selectedSection.set(null); // Sẽ kích hoạt listener, clear Cột 3

        } catch (IOException e) {
            logger.error("Không thể tải export template: {}", name, e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tạo một template mới (trống)
     */
    public void createNewTemplate() {
        currentTemplate = new ExportTemplate();
        currentTemplate.setTemplateName("New Export Template");
        currentTemplate.setSections(new ArrayList<>());

        templateName.set(currentTemplate.getTemplateName());
        currentSections.clear();
        selectedSection.set(null); // Sẽ kích hoạt listener, clear Cột 3
    }

    /**
     * Logic nghiệp vụ khi nhấn nút "Save" (UC-CFG-03, Bước 14.0)
     */
    public void saveTemplate() {
        if (currentTemplate == null) {
            logger.warn("Không có template nào được chọn để lưu.");
            return;
        }

        /**
         * Cập nhật POJO từ các binding
         */
        currentTemplate.setTemplateName(templateName.get());
        currentTemplate.setSections(new ArrayList<>(currentSections));

        try {
            templateService.saveExportTemplate(currentTemplate);
            projectStateService.setStatusMessage("Đã lưu template: " + currentTemplate.getTemplateName());
            logger.info("Lưu export template thành công");

            /**
             * Tải lại danh sách (list)
             */
            loadTemplateNames();

        } catch (IOException e) {
            logger.error("Lỗi lưu export template", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Thêm một chương (section) mới (mặc định là Static)
     */
    public void addSection() {
        if (currentTemplate == null) return;
        ExportTemplateSection newSection = new ExportTemplateSection();
        newSection.setTitle("New Section");
        newSection.setType("Static");
        newSection.setContent("# New Section\n\nNội dung Tĩnh (Static Content).");
        newSection.setQuery(new HashMap<>());
        newSection.setDisplayFormat("FullContent");

        currentSections.add(newSection);
        selectedSection.set(newSection); // Sẽ kích hoạt listener, nạp Cột 3
    }

    /**
     * Xóa chương (section) đang chọn
     */
    public void removeSection() {
        if (selectedSection.get() != null) {
            currentSections.remove(selectedSection.get());
            selectedSection.set(null); // Sẽ kích hoạt listener, clear Cột 3
        }
    }
}