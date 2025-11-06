package com.rms.app.service;

import com.rms.app.model.Artifact;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface (cho DIP) của Lớp Chỉ mục SQLite.
 * Chịu trách nhiệm ĐỌC/GHI vào file index.db.
 * Tham chiếu Kế hoạch Ngày 18 (Giai đoạn 4).
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
     * Thêm một liên kết (link) vào bảng chỉ mục.
     *
     * @param fromId ID của artifact (ví dụ: "UC001")
     * @param toId   ID của artifact được liên kết (ví dụ: "BR001")
     * @throws SQLException Nếu lỗi CSDL
     */
    void insertLink(String fromId, String toId) throws SQLException;

    /**
     * Truy vấn chỉ mục (dùng cho Autocomplete).
     *
     * @param query Từ khóa tìm kiếm
     * @return Danh sách các Artifact (chỉ chứa ID, Name)
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Artifact> queryArtifacts(String query) throws SQLException;

    /**
     * Truy vấn chỉ mục (dùng cho Backlinks).
     *
     * @param artifactId ID của artifact (ví dụ: "BR001")
     * @return Danh sách các Artifact (chỉ chứa ID, Name) liên kết ĐẾN artifactId
     * @throws SQLException Nếu lỗi CSDL
     */
    List<Artifact> queryBacklinks(String artifactId) throws SQLException;
}