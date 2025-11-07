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
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * "Dumb" View Controller cho DashboardView.fxml (Kanban).
 * Chịu trách nhiệm render các cột (column) và thẻ (card)
 * và xử lý sự kiện Kéo-Thả (Drag-and-Drop) (Ngày 29).
 */
public class DashboardView {

    private static final Logger logger = LoggerFactory.getLogger(DashboardView.class);
    @FXML private HBox kanbanContainer;
    private final DashboardViewModel viewModel;

    /**
     * DataFormat tùy chỉnh để truyền Artifact ID khi kéo.
     */
    private static final DataFormat ARTIFACT_DATA_FORMAT = new DataFormat("com.rms.app.model.Artifact");

    @Inject
    public DashboardView(DashboardViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        /**
         * Lắng nghe sự thay đổi dữ liệu (data) trên ViewModel.
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

        createColumn(data, "Draft");
        createColumn(data, "In Review");
        createColumn(data, "Approved");

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
        VBox column = new VBox(5);
        column.setMinWidth(280);
        column.setPrefWidth(280);
        column.setStyle("-fx-background-color: -fx-control-inner-background; -fx-background-radius: 8px;");

        column.setUserData(status);

        Label title = new Label(status + " (" + artifacts.size() + ")");
        /**
         * [SỬA LỖI NGÀY 29]
         * Xử lý lỗi (Error 2) CSS Loop.
         * Loại bỏ thuộc tính "-fx-text-fill: -fx-text-fill;"
         * vì nó gây ra lỗi lặp (loop) trong trình phân tích CSS của JavaFX.
         * Label sẽ tự động kế thừa (inherit) màu chữ (text-fill)
         * từ .root (trong dark-theme.css).
         */
        title.setStyle("-fx-font-weight: bold; -fx-padding: 8px;");
        column.getChildren().add(title);

        VBox cardsContainer = new VBox(8);
        cardsContainer.setPadding(new Insets(5));

        for (Artifact artifact : artifacts) {
            cardsContainer.getChildren().add(createCard(artifact));
        }

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        column.getChildren().add(scrollPane);

        setupDropTarget(column);

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

        /**
         * Thiết lập Thẻ làm Nguồn Kéo (Drag Source) (F-MGT-03)
         */
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();

            content.put(ARTIFACT_DATA_FORMAT, artifact);
            content.putString(artifact.getId());
            db.setContent(content);
            event.consume();
        });

        return card;
    }

    /**
     * Helper thiết lập các trình xử lý (handler) sự kiện
     * cho Cột (Column) khi là Mục tiêu Thả (Drop Target).
     *
     * @param column Cột (VBox)
     */
    private void setupDropTarget(VBox column) {
        /**
         * Sự kiện (Event) khi một Thẻ (Card) được kéo VÀO Cột
         */
        column.setOnDragOver(event -> {
            if (event.getGestureSource() != column && event.getDragboard().hasContent(ARTIFACT_DATA_FORMAT)) {
                /**
                 * Chấp nhận Thẻ (Card)
                 */
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        /**
         * Sự kiện (Event) khi Thẻ (Card) được THẢ (dropped) vào Cột
         */
        column.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasContent(ARTIFACT_DATA_FORMAT)) {
                Artifact droppedArtifact = (Artifact) db.getContent(ARTIFACT_DATA_FORMAT);
                String newStatus = (String) column.getUserData();

                logger.info("BA đã thả (drop) thẻ {} vào cột {}", droppedArtifact.getId(), newStatus);

                /**
                 * Gọi ViewModel để xử lý logic nghiệp vụ (F-MGT-03)
                 */
                viewModel.updateArtifactStatus(droppedArtifact, newStatus);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
}