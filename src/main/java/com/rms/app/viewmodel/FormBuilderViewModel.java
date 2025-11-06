package com.rms.app.viewmodel;

import com.google.inject.Inject;
import com.rms.app.model.ArtifactTemplate;
import com.rms.app.service.ITemplateService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import com.rms.app.service.IProjectStateService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

// "Brain" - Logic UI cho FormBuilderView
public class FormBuilderViewModel {
    private static final Logger logger = LoggerFactory.getLogger(FormBuilderViewModel.class);

    private final ITemplateService templateService;
    private final IProjectStateService projectStateService;

    // Properties mà View sẽ bind vào
    public final StringProperty templateName = new SimpleStringProperty("New Template");
    public final StringProperty prefixId = new SimpleStringProperty("NEW");
    public final ObservableList<String> toolboxItems = FXCollections.observableArrayList(
            // Các loại trường F-CFG-02
            "Text (Single-line)", "Text Area (Multi-line)", "Dropdown", "Linker (@ID)", "Flow Builder"
    );
    public final ObservableList<ArtifactTemplate.FieldTemplate> currentFields = FXCollections.observableArrayList();


    @Inject
    public FormBuilderViewModel(ITemplateService templateService, IProjectStateService projectStateService) {
        this.templateService = templateService;
        this.projectStateService = projectStateService;
    }

    // Logic nghiệp vụ khi nhấn nút Save
    public void saveTemplate() {
        ArtifactTemplate template = new ArtifactTemplate();
        template.setTemplateName(templateName.get());
        template.setPrefixId(prefixId.get());
        template.setFields(new ArrayList<>(currentFields)); // Chuyển đổi ObservableList thành List

        try {
            // 8.0. BA nhấn "Save"
            templateService.saveTemplate(template);
            projectStateService.setStatusMessage("Đã lưu template: " + template.getTemplateName());
            logger.info("Lưu template thành công");
        } catch (IOException e) {
            logger.error("Lỗi lưu template", e);
            projectStateService.setStatusMessage("Lỗi: " + e.getMessage());
        }
    }
}