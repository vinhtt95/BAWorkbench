package com.rms.app.view;

import com.rms.app.model.FlowStep;
import com.rms.app.viewmodel.ArtifactViewModel; // [THÊM MỚI] Import
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Controller cho FXML control FlowBuilderControl.fxml.
 * Chịu trách nhiệm quản lý giao diện người dùng cho việc tạo và chỉnh sửa
 * các bước (FlowStep) trong một quy trình (flow).
 */
public class FlowBuilderControl {

    private static final Logger logger = LoggerFactory.getLogger(FlowBuilderControl.class);

    @FXML private TableView<FlowStep> stepsTableView;
    @FXML private TableColumn<FlowStep, String> stepActorColumn;
    @FXML private TableColumn<FlowStep, String> stepActionColumn;

    private ObservableList<FlowStep> flowStepsList;
    private boolean isInitialized = false;

    /**
     * [THÊM MỚI] Tham chiếu đến ViewModel cha (parent)
     * (được inject (tiêm) bởi RenderService)
     */
    private ArtifactViewModel viewModel;

    /**
     * Hàm này được RenderService gọi để "inject" ViewModel cha (parent).
     *
     * @param viewModel ViewModel của Artifact
     */
    public void setViewModel(ArtifactViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Hàm này được RenderService gọi để "inject" danh sách (list)
     * từ ArtifactViewModel vào control này.
     * Đây là nơi an toàn để cấu hình các cột (column).
     *
     * @param flowSteps Danh sách các bước quy trình có thể quan sát (ObservableList).
     */
    public void setData(ObservableList<FlowStep> flowSteps) {
        this.flowStepsList = flowSteps;
        this.stepsTableView.setItems(this.flowStepsList);

        if (!isInitialized) {
            try {
                stepActorColumn.setCellFactory(TextFieldTableCell.forTableColumn());
                stepActorColumn.setOnEditCommit(event -> {
                    event.getRowValue().setActor(event.getNewValue());
                });
                stepActorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getActor()));

                stepActionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
                stepActionColumn.setOnEditCommit(event -> {
                    event.getRowValue().setAction(event.getNewValue());
                });
                stepActionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAction()));

                isInitialized = true;
            } catch (NullPointerException e) {
                logger.error("LỖI FXML: Các TableColumn (stepActorColumn, stepActionColumn) bị null. " +
                        "Kiểm tra lại file FlowBuilderControl.fxml và fx:id.", e);
            }
        }
    }

    /**
     * [THÊM MỚI] Xử lý sự kiện nhấn nút "Gemini: Đề xuất".
     * Tuân thủ UC-DEV-03 (Luồng 1.0.A1).
     */
    @FXML
    private void handleGeminiSuggest() {
        if (viewModel != null) {
            viewModel.generateFlowFromGemini();
        } else {
            logger.error("Không thể gọi Gemini: ArtifactViewModel là null.");
        }
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Add Step".
     */
    @FXML
    private void handleAddStep() {
        if (flowStepsList != null) {
            FlowStep newStep = new FlowStep();
            newStep.setActor("Actor");
            newStep.setAction("New Action");
            flowStepsList.add(newStep);
            stepsTableView.getSelectionModel().select(newStep);
        } else {
            logger.warn("handleAddStep thất bại: flowStepsList là null.");
        }
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Remove Selected".
     */
    @FXML
    private void handleRemoveStep() {
        FlowStep selected = stepsTableView.getSelectionModel().getSelectedItem();
        if (selected != null && flowStepsList != null) {
            flowStepsList.remove(selected);
        }
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Add If/Then".
     */
    @FXML
    private void handleAddLogic() {
        if (flowStepsList != null) {
            FlowStep ifStep = new FlowStep();
            ifStep.setLogicType("IF");
            ifStep.setActor("Logic");
            ifStep.setAction("IF (...)");
            flowStepsList.add(ifStep);

            stepsTableView.getSelectionModel().select(ifStep);
        } else {
            logger.warn("handleAddLogic thất bại: flowStepsList là null.");
        }
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Move Up".
     */
    @FXML
    private void handleMoveUp() {
        int index = stepsTableView.getSelectionModel().getSelectedIndex();
        if (index > 0 && flowStepsList != null) {
            Collections.swap(flowStepsList, index, index - 1);
            stepsTableView.getSelectionModel().select(index - 1);
        }
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Move Down".
     */
    @FXML
    private void handleMoveDown() {
        int index = stepsTableView.getSelectionModel().getSelectedIndex();
        if (index != -1 && index < flowStepsList.size() - 1 && flowStepsList != null) {
            Collections.swap(flowStepsList, index, index + 1);
            stepsTableView.getSelectionModel().select(index + 1);
        }
    }
}