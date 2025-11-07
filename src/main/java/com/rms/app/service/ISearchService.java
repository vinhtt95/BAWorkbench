package com.rms.app.service;

import com.rms.app.model.Artifact;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface cho DIP (SOLID)
 */
public interface ISearchService {

    /**
     * Quét thư mục Artifacts/ và xây dựng index
     */
    void buildIndex() throws IOException;

    /**
     * Tìm kiếm cache (index) cho autocomplete
     *
     * @param query (ví dụ: "@BR")
     * @return Danh sách các Artifacts khớp
     */
    List<Artifact> search(String query);

    /**
     * Tìm tất cả các artifact liên kết (link) đến artifactId này.
     * Tham chiếu UC-MOD-03.
     *
     * @param artifactId ID của artifact (ví dụ: "UC001")
     * @return Danh sách các Artifacts có chứa link "@UC001"
     */
    List<Artifact> getBacklinks(String artifactId);

    /**
     * [THÊM MỚI NGÀY 28]
     * Lấy tất cả các artifact, được nhóm (grouped) theo Trạng thái (Status)
     * để hiển thị trên Bảng Kanban (UC-MGT-02).
     *
     * @return Map (ánh xạ) {StatusName -> List<Artifact>}
     */
    Map<String, List<Artifact>> getArtifactsGroupedByStatus();
}