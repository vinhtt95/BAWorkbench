package com.rms.app.service;

import com.rms.app.model.Artifact;
import com.rms.app.model.ProjectFolder;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interface (cho DIP) của Lớp Chỉ mục SQLite.
 * [CẬP NHẬT] Hỗ trợ cấu trúc cây thư mục (đa cấp).
 */
public interface ISqliteIndexRepository {

    /**
     * Khởi tạo kết nối và tạo các bảng (tables) nếu chúng chưa tồn tại.
     *
     * @param projectConfigFile Thư mục .config của dự án
     * @throws SQLException Nếu lỗi CSDL
     */
    void initializeDatabase(File projectConfigFile) throws SQLException;

    /**
     * Xóa sạch (TRUNCATE) tất cả dữ liệu khỏi các bảng chỉ mục.
     *
     * @throws SQLException Nếu lỗi CSDL
     */
    void clearIndex() throws SQLException;

    /**
     * Thêm (hoặc cập nhật) thông tin của một artifact vào bảng chỉ mục.
     *
     * @param artifact Đối tượng (Artifact) đã được load
     * @throws SQLException Nếu lỗi CSDL
     */
    void insertArtifact(Artifact artifact) throws SQLException;

    /**
     * [MỚI] Thêm (hoặc cập nhật) một thư mục vào bảng chỉ mục.
     *
     * @param folder Đối tượng Thư mục
     * @throws SQLException Nếu lỗi CSDL
     */
    void insertFolder(ProjectFolder folder) throws SQLException;

    /**
     * Thêm một liên kết (link) vào bảng chỉ mục.
     *
     * @param fromId ID của artifact (ví dụ: "UC001")
     * @param toId   ID của artifact được liên kết (ví dụ: "BR001")
     * @throws SQLException Nếu lỗi CSDL
     */
    void insertLink(String fromId, String toId) throws SQLException;

    /**
     * Xóa một artifact khỏi bảng 'artifacts'.
     *
     * @param artifactId ID của artifact cần xóa
     * @throws SQLException Nếu lỗi CSDL
     */
    void deleteArtifact(String artifactId) throws SQLException;

    /**
     * [MỚI] Xóa một thư mục (và tất cả con của nó) khỏi bảng 'folders'.
     *
     * @param folderId ID của thư mục cần xóa
     * @throws SQLException Nếu lỗi CSDL
     */
    void deleteFolder(String folderId) throws SQLException;


    /**
     * Xóa tất cả các liên kết (links) TỪ một artifact.
     *
     * @param artifactId ID của artifact (nguồn link)
     * @throws SQLException Nếu lỗi CSDL
     */
    void deleteLinksForArtifact(String artifactId) throws SQLException;

    /**
     * Truy vấn chỉ mục (dùng cho Autocomplete).
     *
     * @param query Từ khóa tìm kiếm
     * @return Danh sách các Artifact (chỉ chứa ID, Name, relativePath)
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Artifact> queryArtifacts(String query) throws SQLException;

    /**
     * Truy vấn chỉ mục (dùng cho Backlinks).
     *
     * @param artifactId ID của artifact (ví dụ: "BR001")
     * @return Danh sách các Artifact (chỉ chứa ID, Name, relativePath) liên kết ĐẾN artifactId
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Artifact> queryBacklinks(String artifactId) throws SQLException;

    /**
     * Lấy danh sách các Trạng thái (Status) duy nhất (distinct)
     * (dùng cho Kanban).
     *
     * @return Danh sách các tên Trạng thái
     * @throws SQLException Nếu lỗi CSDL
     */
    List<String> getDefinedStatuses() throws SQLException;

    /**
     * Lấy tất cả các artifact khớp với một Trạng thái (Status) cụ thể
     * (dùng cho Kanban).
     *
     * @param status Tên Trạng thái
     * @return Danh sách các Artifact
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Artifact> getArtifactsByStatus(String status) throws SQLException;

    /**
     * [MỚI] Lấy tất cả các Thư mục con (sub-folder) trực tiếp.
     *
     * @param parentFolderId ID của thư mục cha (hoặc null cho gốc)
     * @return Danh sách các ProjectFolder
     * @throws SQLException Nếu lỗi CSDL
     */
    List<ProjectFolder> getFolders(String parentFolderId) throws SQLException;

    /**
     * [MỚI] Lấy tất cả các Artifact (file) trực tiếp trong một thư mục.
     *
     * @param parentFolderId ID của thư mục cha (hoặc null cho gốc)
     * @return Danh sách các Artifact
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Artifact> getArtifacts(String parentFolderId) throws SQLException;

    /**
     * [ĐÃ THÊM LẠI] Lấy tất cả các artifact (chỉ ID, Name, Type, relativePath)
     * khớp với một Loại (Type) cụ thể (dùng cho Xuất Excel).
     *
     * @param type Tên Loại (ví dụ: "UC")
     * @return Danh sách các Artifact
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Artifact> getArtifactsByType(String type) throws SQLException;


    /**
     * Lấy danh sách các Loại (Type) duy nhất (distinct)
     * (dùng cho Export Query Builder).
     *
     * @return Danh sách các tên Loại (ví dụ: "UC", "BR", "TSK")
     * @throws SQLException Nếu lỗi CSDL
     */
    List<String> getDefinedTypes() throws SQLException;

    /**
     * Truy vấn CSDL chỉ mục (SQLite) cho Trình xuất bản (Publisher).
     *
     * @param type       Loại artifact (ví dụ: "UC")
     * @param status     Trạng thái artifact (ví dụ: "Approved", có thể null)
     * @param releaseId  ID của Release (ví dụ: "REL001", có thể null)
     * @return Danh sách Artifacts
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Artifact> queryArtifactsByCriteria(String type, String status, String releaseId) throws SQLException;

    /**
     * Lấy tất cả các Nút (Node) cho Sơ đồ Quan hệ (Graph).
     * (UC-MOD-02)
     *
     * @return Danh sách các Map (Ánh xạ) {id, label, group}
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Map<String, String>> getAllNodes() throws SQLException;

    /**
     * Lấy tất cả các Cạnh (Edge) cho Sơ đồ Quan hệ (Graph).
     * (UC-MOD-02)
     *
     * @return Danh sách các Map (Ánh xạ) {from, to}
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Map<String, String>> getAllEdges() throws SQLException;
}