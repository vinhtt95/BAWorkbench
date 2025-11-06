package com.rms.app.view;

import com.rms.app.model.FlowStep;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

// Controller cho FXML Component ở trên
public class FlowBuilderControl {

    @FXML private TableView<FlowStep> stepsTableView;
    @FXML private TableColumn<FlowStep, String> stepActorColumn;
    @FXML private TableColumn<FlowStep, String> stepActionColumn;

    private ObservableList<FlowStep> flowStepsList; // Sẽ được inject từ ViewModel

    @FXML
    public void initialize() {
        // Cấu hình TableView có thể chỉnh sửa (editable)
        stepActorColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        stepActorColumn.setOnEditCommit(event -> {
            event.getRowValue().setActor(event.getNewValue());
        });
        // Ánh xạ dữ liệu vào cột
        stepActorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getActor()));

        stepActionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        stepActionColumn.setOnEditCommit(event -> {
            event.getRowValue().setAction(event.getNewValue());
        });
        stepActionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAction()));
    }

    /**
     * Hàm này được RenderService gọi để "inject" danh sách (list)
     * từ ArtifactViewModel vào control này.
     */
    public void setData(ObservableList<FlowStep> flowSteps) {
        this.flowStepsList = flowSteps;
        stepsTableView.setItems(this.flowStepsList);
    }

    // --- Logic cho các nút bấm (Ngày 15) ---

    @FXML
    private void handleAddStep() {
        if (flowStepsList != null) {
            FlowStep newStep = new FlowStep();
            newStep.setActor("Actor");
            newStep.setAction("New Action");
            flowStepsList.add(newStep);
        }
    }

    @FXML
    private void handleRemoveStep() {
        FlowStep selected = stepsTableView.getSelectionModel().getSelectedItem();
        if (selected != null && flowStepsList != null) {
            flowStepsList.remove(selected);
        }
    }

    @FXML
    private void handleAddLogic() {
        // TODO (Ngày 15)
    }

    @FXML
    private void handleMoveUp() {
        // TODO (Ngày 15)
    }

    @FXML
    private void handleMoveDown() {
        // TODO (Ngày 15)
    }
}