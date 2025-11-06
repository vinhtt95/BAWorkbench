package com.rms.app.service;

import com.rms.app.model.Artifact;
import java.io.IOException;
import java.util.List;

// Interface cho DIP (SOLID)
public interface ISearchService {

    /**
     * Quét thư mục Artifacts/ và xây dựng index
     *
     */
    void buildIndex() throws IOException;

    /**
     * Tìm kiếm cache (index) cho autocomplete
     *
     * @param query (ví dụ: "@BR")
     * @return Danh sách các Artifacts khớp
     */
    List<Artifact> search(String query);

    // (Sẽ thêm getBacklinks(String id) ở Ngày 21)
}