package com.rms.app.view;

import com.google.inject.Inject;
import com.rms.app.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;

public class MainView {

    // 1. FXML Controls (Inject bởi FXMLLoader)
    @FXML
    private TreeView<String> projectTreeView;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Accordion rightAccordion;

    // 2. ViewModel (Inject bởi Guice)
    private final MainViewModel viewModel;

    // Tuân thủ DIP [C-03], ViewModel được inject
    @Inject
    public MainView(MainViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // 3. Initialize (Nơi View bind vào ViewModel)
    @FXML
    public void initialize() {
        // Đây là nơi binding sẽ diễn ra
        // Ví dụ (sẽ được implement ở Giai đoạn 1):
        // projectTreeView.setRoot(viewModel.getProjectRoot());
        // mainTabPane.getTabs().bind(viewModel.getOpenTabs());

        System.out.println("MainView initialized. ViewModel is: " + viewModel);
    }
}