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
     *
     * @throws IOException Nếu lỗi I/O
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
     * Lấy tất cả các artifact, được nhóm (grouped) theo Trạng thái (Status)
     * để hiển thị trên Bảng Kanban (UC-MGT-02).
     *
     * @return Map (ánh xạ) {StatusName -> List<Artifact>}
     */
    Map<String, List<Artifact>> getArtifactsGroupedByStatus();

    /**
     * Cập nhật Trạng thái (Status) của một artifact.
     * Tuân thủ UC-MGT-02 (Luồng 1.0, Bước 7.0) và F-MGT-03.
     *
     * @param artifact   Đối tượng (chỉ chứa ID, Type) được kéo
     * @param newStatus  Trạng thái (Status) mới (tên của cột được thả vào)
     * @throws IOException Nếu lỗi load hoặc save (Triple-Write)
     */
    void updateArtifactStatus(Artifact artifact, String newStatus) throws IOException;

    /**
     * [THÊM MỚI] Lấy (fetch) tất cả Nút (Node) và Cạnh (Edge)
     * từ CSDL Chỉ mục (Index DB).
     * Tuân thủ UC-MOD-02 (Ngày 36).
     *
     * @return Map (Ánh xạ) chứa "nodes" và "edges"
     * @throws IOException Nếu lỗi CSDL (SQL)
     */
    Map<String, List<Map<String, String>>> getGraphData() throws IOException;
}