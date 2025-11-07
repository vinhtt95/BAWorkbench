package com.rms.app.repository;

import com.google.inject.Singleton;
import com.rms.app.model.Artifact;
import com.rms.app.model.ProjectFolder; // Phải tạo model này
import com.rms.app.service.ISqliteIndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai (implementation) logic I/O cho CSDL Chỉ mục (SQLite).
 * [CẬP NHẬT] Hỗ trợ cấu trúc cây thư mục (đa cấp).
 */
@Singleton
public class SqliteIndexRepository implements ISqliteIndexRepository {

    private static final Logger logger = LoggerFactory.getLogger(SqliteIndexRepository.class);
    private String connectionString = null;

    /**
     * Tạo kết nối đến CSDL SQLite.
     *
     * @return một đối tượng Connection
     * @throws SQLException Nếu không thể kết nối
     */
    private Connection connect() throws SQLException {
        if (connectionString == null) {
            throw new SQLException("Database chưa được khởi tạo. Phải gọi initializeDatabase() trước.");
        }
        return DriverManager.getConnection(connectionString);
    }

    @Override
    public void initializeDatabase(File projectConfigDir) throws SQLException {
        File dbFile = new File(projectConfigDir, "index.db");
        this.connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        logger.info("Đang khởi tạo CSDL Chỉ mục tại: {}", dbFile.getAbsolutePath());

        /**
         * [MỚI] Bảng Folders
         * artifactTypeScope: Loại artifact nào được phép (ví dụ: "UC", "BR"). Null nghĩa là thư mục gốc.
         */
        String createFoldersTable = "CREATE TABLE IF NOT EXISTS folders ("
                + " id TEXT PRIMARY KEY,"
                + " name TEXT NOT NULL,"
                + " parentId TEXT," // NULL cho thư mục gốc (UC, BR...)
                + " artifactTypeScope TEXT,"
                + " relativePath TEXT NOT NULL UNIQUE,"
                + " FOREIGN KEY(parentId) REFERENCES folders(id) ON DELETE CASCADE"
                + ");";

        /**
         * [CẬP NHẬT] Bảng Artifacts
         * folderId: Thư mục cha chứa artifact này.
         * relativePath: Đường dẫn vật lý đầy đủ.
         */
        String createArtifactsTable = "CREATE TABLE IF NOT EXISTS artifacts ("
                + " id TEXT PRIMARY KEY,"
                + " name TEXT NOT NULL,"
                + " type TEXT," // (vẫn giữ để lọc nhanh, ví dụ: "UC")
                + " status TEXT,"
                + " folderId TEXT,"
                + " relativePath TEXT NOT NULL UNIQUE,"
                + " FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE SET NULL"
                + ");";

        String createLinksTable = "CREATE TABLE IF NOT EXISTS links ("
                + " fromId TEXT NOT NULL,"
                + " toId TEXT NOT NULL,"
                + " PRIMARY KEY (fromId, toId),"
                + " FOREIGN KEY(fromId) REFERENCES artifacts(id) ON DELETE CASCADE"
                + ");"; // Bảng Links không đổi

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createFoldersTable);
            stmt.execute(createArtifactsTable);
            stmt.execute(createLinksTable);
            logger.info("Khởi tạo bảng 'folders', 'artifacts', và 'links' thành công.");
        }
    }

    @Override
    public void clearIndex() throws SQLException {
        String deleteLinks = "DELETE FROM links;";
        String deleteArtifacts = "DELETE FROM artifacts;";
        String deleteFolders = "DELETE FROM folders;";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(deleteLinks);
            stmt.execute(deleteArtifacts);
            stmt.execute(deleteFolders);
            logger.info("Đã xóa sạch (clear) CSDL Chỉ mục.");
        }
    }

    @Override
    public void insertArtifact(Artifact artifact) throws SQLException {
        String status = "Draft";
        if (artifact.getFields() != null && artifact.getFields().containsKey("Trạng thái")) {
            Object statusObj = artifact.getFields().get("Trạng thái");
            if (statusObj != null) {
                status = statusObj.toString();
            }
        }

        String name = (artifact.getName() != null) ? artifact.getName() :
                (artifact.getId() != null ? artifact.getId() : "Untitled");

        // [CẬP NHẬT] Thêm folderId và relativePath
        String sql = "INSERT OR REPLACE INTO artifacts (id, name, type, status, folderId, relativePath) VALUES(?,?,?,?,?,?);";

        // Tạm thời logic folderId (sẽ được IndexServiceImpl xử lý đúng)
        String folderId = null; // Cần logic để tìm folderId từ relativePath

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artifact.getId());
            pstmt.setString(2, name);
            pstmt.setString(3, artifact.getArtifactType());
            pstmt.setString(4, status);
            pstmt.setString(5, folderId); // Tạm thời null
            pstmt.setString(6, artifact.getRelativePath());
            pstmt.executeUpdate();
        }
    }

    /**
     * [MỚI] Triển khai insertFolder
     */
    @Override
    public void insertFolder(ProjectFolder folder) throws SQLException {
        String sql = "INSERT OR REPLACE INTO folders (id, name, parentId, artifactTypeScope, relativePath) VALUES(?,?,?,?,?);";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, folder.getId());
            pstmt.setString(2, folder.getName());
            pstmt.setString(3, folder.getParentId());
            pstmt.setString(4, folder.getArtifactTypeScope());
            pstmt.setString(5, folder.getRelativePath());
            pstmt.executeUpdate();
        }
    }


    @Override
    public void insertLink(String fromId, String toId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO links (fromId, toId) VALUES(?,?);";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fromId);
            pstmt.setString(2, toId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteArtifact(String artifactId) throws SQLException {
        String sql = "DELETE FROM artifacts WHERE id = ?;";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artifactId);
            pstmt.executeUpdate();
        }
    }

    /**
     * [MỚI] Triển khai deleteFolder
     */
    @Override
    public void deleteFolder(String folderId) throws SQLException {
        // CSDL sẽ tự động xóa các folder con (ON DELETE CASCADE)
        // CSDL sẽ tự động set folderId = NULL cho các artifact con (ON DELETE SET NULL)
        String sql = "DELETE FROM folders WHERE id = ?;";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, folderId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteLinksForArtifact(String artifactId) throws SQLException {
        String sql = "DELETE FROM links WHERE fromId = ?;";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artifactId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Artifact> queryArtifacts(String query) throws SQLException {
        List<Artifact> results = new ArrayList<>();
        // [CẬP NHẬT] Thêm relativePath vào SELECT
        String sql = "SELECT id, name, type, relativePath FROM artifacts "
                + "WHERE id LIKE ? OR name LIKE ? LIMIT 10;";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String wildcardQuery = "%" + query.replace("@", "") + "%";
            pstmt.setString(1, wildcardQuery);
            pstmt.setString(2, wildcardQuery);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Artifact artifact = new Artifact();
                artifact.setId(rs.getString("id"));
                artifact.setName(rs.getString("name"));
                artifact.setArtifactType(rs.getString("type"));
                artifact.setRelativePath(rs.getString("relativePath"));
                results.add(artifact);
            }
        }
        return results;
    }

    @Override
    public List<Artifact> queryBacklinks(String artifactId) throws SQLException {
        List<Artifact> results = new ArrayList<>();
        // [CẬP NHẬT] Thêm relativePath vào SELECT
        String sql = "SELECT a.id, a.name, a.type, a.relativePath FROM artifacts a "
                + "JOIN links l ON a.id = l.fromId "
                + "WHERE l.toId = ?;";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artifactId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Artifact artifact = new Artifact();
                artifact.setId(rs.getString("id"));
                artifact.setName(rs.getString("name"));
                artifact.setArtifactType(rs.getString("type"));
                artifact.setRelativePath(rs.getString("relativePath"));
                results.add(artifact);
            }
        }
        return results;
    }

    @Override
    public List<String> getDefinedStatuses() throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT DISTINCT status FROM artifacts WHERE status IS NOT NULL ORDER BY status;";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("status"));
            }
        }
        return results;
    }

    @Override
    public List<Artifact> getArtifactsByStatus(String status) throws SQLException {
        List<Artifact> results = new ArrayList<>();
        // [CẬP NHẬT] Thêm relativePath
        String sql = "SELECT id, name, type, relativePath FROM artifacts WHERE status = ?;";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Artifact artifact = new Artifact();
                artifact.setId(rs.getString("id"));
                artifact.setName(rs.getString("name"));
                artifact.setArtifactType(rs.getString("type"));
                artifact.setRelativePath(rs.getString("relativePath"));
                results.add(artifact);
            }
        }
        return results;
    }

    /**
     * [MỚI] Triển khai getFolders
     */
    @Override
    public List<ProjectFolder> getFolders(String parentFolderId) throws SQLException {
        List<ProjectFolder> results = new ArrayList<>();
        String sql;
        if (parentFolderId == null) {
            sql = "SELECT id, name, parentId, artifactTypeScope, relativePath FROM folders WHERE parentId IS NULL ORDER BY name;";
        } else {
            sql = "SELECT id, name, parentId, artifactTypeScope, relativePath FROM folders WHERE parentId = ? ORDER BY name;";
        }

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (parentFolderId != null) {
                pstmt.setString(1, parentFolderId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ProjectFolder folder = new ProjectFolder();
                folder.setId(rs.getString("id"));
                folder.setName(rs.getString("name"));
                folder.setParentId(rs.getString("parentId"));
                folder.setArtifactTypeScope(rs.getString("artifactTypeScope"));
                folder.setRelativePath(rs.getString("relativePath"));
                results.add(folder);
            }
        }
        return results;
    }

    /**
     * [MỚI] Triển khai getArtifacts
     */
    @Override
    public List<Artifact> getArtifacts(String parentFolderId) throws SQLException {
        List<Artifact> results = new ArrayList<>();
        String sql;
        if (parentFolderId == null) {
            sql = "SELECT id, name, type, relativePath FROM artifacts WHERE folderId IS NULL ORDER BY name;";
        } else {
            sql = "SELECT id, name, type, relativePath FROM artifacts WHERE folderId = ? ORDER BY name;";
        }

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (parentFolderId != null) {
                pstmt.setString(1, parentFolderId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Artifact artifact = new Artifact();
                artifact.setId(rs.getString("id"));
                artifact.setName(rs.getString("name"));
                artifact.setArtifactType(rs.getString("type"));
                artifact.setRelativePath(rs.getString("relativePath"));
                results.add(artifact);
            }
        }
        return results;
    }


    /**
     * [ĐÃ CẬP NHẬT] Phương thức này vẫn hữu ích cho việc lọc (filter)
     * (ví dụ: Export Excel). Nó chỉ không dùng để xây dựng cây nữa.
     */
    @Override
    public List<Artifact> getArtifactsByType(String type) throws SQLException {
        List<Artifact> results = new ArrayList<>();
        String sql = "SELECT id, name, type, relativePath FROM artifacts WHERE type = ?;";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Artifact artifact = new Artifact();
                artifact.setId(rs.getString("id"));
                artifact.setName(rs.getString("name"));
                artifact.setArtifactType(rs.getString("type"));
                artifact.setRelativePath(rs.getString("relativePath"));
                results.add(artifact);
            }
        }
        return results;
    }

    /**
     * Triển khai logic query (UC-CFG-03)
     */
    @Override
    public List<String> getDefinedTypes() throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT DISTINCT type FROM artifacts WHERE type IS NOT NULL ORDER BY type;";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("status"));
            }
        }
        return results;
    }

    /**
     * Triển khai logic query (UC-PUB-01)
     */
    @Override
    public List<Artifact> queryArtifactsByCriteria(String type, String status, String releaseId) throws SQLException {
        List<Artifact> results = new ArrayList<>();

        /**
         * Xây dựng (build) query SQL động
         */
        StringBuilder sql = new StringBuilder("SELECT id, name, type, relativePath FROM artifacts a WHERE a.type = ?");
        List<Object> params = new ArrayList<>();
        params.add(type);

        if (status != null && !status.isEmpty()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }

        if (releaseId != null && !releaseId.isEmpty()) {
            /**
             * Lọc các artifact (a) có một liên kết (link) TỚI releaseId
             */
            sql.append(" AND EXISTS (SELECT 1 FROM links l WHERE l.fromId = a.id AND l.toId = ?)");
            params.add(releaseId);
        }

        sql.append(" ORDER BY a.id;");

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            /**
             * Đặt (set) các tham số (parameter)
             */
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Artifact artifact = new Artifact();
                artifact.setId(rs.getString("id"));
                artifact.setName(rs.getString("name"));
                artifact.setArtifactType(rs.getString("type"));
                artifact.setRelativePath(rs.getString("relativePath"));
                results.add(artifact);
            }
        }
        return results;
    }

    /**
     * [KHÔNG THAY ĐỔI] Logic Graph View không cần thay đổi
     */
    @Override
    public List<Map<String, String>> getAllNodes() throws SQLException {
        List<Map<String, String>> results = new ArrayList<>();
        /**
         * 'label' và 'group' là các key (khóa) được vis.js yêu cầu
         */
        String sql = "SELECT id, name AS label, type AS 'group' FROM artifacts;";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> node = new HashMap<>();
                node.put("id", rs.getString("id"));
                node.put("label", rs.getString("label"));
                node.put("group", rs.getString("group"));
                results.add(node);
            }
        }
        return results;
    }

    /**
     * [KHÔNG THAY ĐỔI] Logic Graph View không cần thay đổi
     */
    @Override
    public List<Map<String, String>> getAllEdges() throws SQLException {
        List<Map<String, String>> results = new ArrayList<>();
        /**
         * 'from' và 'to' là các key (khóa) được vis.js yêu cầu
         */
        String sql = "SELECT fromId AS 'from', toId AS 'to' FROM links;";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> edge = new HashMap<>();
                edge.put("from", rs.getString("from"));
                edge.put("to", rs.getString("to"));
                results.add(edge);
            }
        }
        return results;
    }
}