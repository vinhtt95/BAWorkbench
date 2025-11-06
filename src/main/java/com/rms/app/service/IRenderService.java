package com.rms.app.service;

import com.rms.app.model.ArtifactTemplate;
import com.rms.app.viewmodel.ArtifactViewModel;
import javafx.scene.Node;

import java.util.List;

// Interface cho DIP (SOLID)
public interface IRenderService {

    /**
     * Đọc template và sinh ra danh sách các control JavaFX.
     * Tham chiếu UC-CFG-01, UC-DEV-01
     * @param template Template để render
     * @param viewModel ViewModel để bind dữ liệu vào
     * @return Danh sách các Node (Label, TextField, v.v.)
     */
    List<Node> renderForm(ArtifactTemplate template, ArtifactViewModel viewModel);
}