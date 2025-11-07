package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.viewmodel.DashboardViewModel;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * "Dumb" View Controller cho DashboardView.fxml (Kanban).
 * Chịu trách nhiệm render các cột (column) và thẻ (card)
 * dựa trên dữ liệu từ DashboardViewModel.
 */
public class DashboardView {

    @FXML private HBox kanbanContainer;

    private final DashboardViewModel viewModel;

    @Inject
    public DashboardView(DashboardViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        /**
         * Lắng nghe sự thay đổi dữ liệu (data) trên ViewModel.
         * Khi dữ liệu thay đổi, render lại toàn bộ bảng Kanban.
         */
        viewModel.getArtifactsByStatus().addListener((MapChangeListener<String, List<Artifact>>) change -> {
            renderKanbanBoard();
        });

        /**
         * Kích hoạt tải dữ liệu lần đầu tiên.
         */
        viewModel.loadKanbanData();
    }

    /**
     * Tự động tạo (render) các Cột (Column) và Thẻ (Card)
     * dựa trên dữ liệu (Map) từ ViewModel.
     */
    private void renderKanbanBoard() {
        kanbanContainer.getChildren().clear();

        Map<String, List<Artifact>> data = viewModel.getArtifactsByStatus();

        /**
         * Hiển thị các cột theo thứ tự ưu tiên: Draft, In Review, Approved
         */
        createColumn(data, "Draft");
        createColumn(data, "In Review");
        createColumn(data, "Approved");

        /**
         * Hiển thị các cột (status) còn lại (nếu có)
         */
        data.keySet().stream()
                .filter(status -> !"Draft".equals(status) && !"In Review".equals(status) && !"Approved".equals(status))
                .sorted()
                .forEach(status -> createColumn(data, status));
    }

    /**
     * Helper tạo một Cột (VBox) cho một Trạng thái (Status) cụ thể.
     *
     * @param data Map (ánh xạ) dữ liệu
     * @param status Tên của trạng thái (ví dụ: "Draft")
     */
    private void createColumn(Map<String, List<Artifact>> data, String status) {
        if (!data.containsKey(status)) {
            return;
        }

        List<Artifact> artifacts = data.get(status);

        /**
         * Tạo Vỏ (Wrapper) của Cột
         */
        VBox column = new VBox(5);
        column.setMinWidth(280);
        column.setPrefWidth(280);
        column.setStyle("-fx-background-color: -fx-control-inner-background; -fx-background-radius: 8px;");

        /**
         * Tiêu đề Cột
         */
        Label title = new Label(status + " (" + artifacts.size() + ")");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 8px; -fx-text-fill: -fx-text-fill;");
        column.getChildren().add(title);

        /**
         * Vùng chứa Thẻ (Card)
         */
        VBox cardsContainer = new VBox(8);
        cardsContainer.setPadding(new Insets(5));

        /**
         * Tạo Thẻ (Card) cho mỗi Artifact
         */
        for (Artifact artifact : artifacts) {
            cardsContainer.getChildren().add(createCard(artifact));
        }

        /**
         * Thêm ScrollPane nếu Cột quá cao
         */
        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        column.getChildren().add(scrollPane);

        kanbanContainer.getChildren().add(column);
    }

    /**
     * Helper tạo một Thẻ (Card) (VBox) cho một Artifact.
     *
     * @param artifact Dữ liệu Artifact
     * @return Node (VBox) đại diện cho Thẻ
     */
    private Node createCard(Artifact artifact) {
        VBox card = new VBox(3);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: -fx-base; -fx-background-radius: 4px; -fx-border-width: 1px; -fx-border-color: #3c3f41;");

        Label idLabel = new Label(artifact.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-accent;");

        Label nameLabel = new Label(artifact.getName());
        nameLabel.setWrapText(true);

        card.getChildren().addAll(idLabel, nameLabel);
        return card;
    }
}