package com.rms.app.repository;

import com.google.inject.Singleton;
import com.rms.app.model.Artifact;
import com.rms.app.service.ISqliteIndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai (implementation) logic I/O cho CSDL Chỉ mục (SQLite).
 * Chỉ chịu trách nhiệm ĐỌC/GHI CSDL thô.
 * Tham chiếu Kế hoạch Ngày 18 (Giai đoạn 4).
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

        String createArtifactsTable = "CREATE TABLE IF NOT EXISTS artifacts ("
                + " id TEXT PRIMARY KEY,"
                + " name TEXT NOT NULL,"
                + " type TEXT,"
                + " status TEXT"
                + ");";

        String createLinksTable = "CREATE TABLE IF NOT EXISTS links ("
                + " fromId TEXT NOT NULL,"
                + " toId TEXT NOT NULL,"
                + " PRIMARY KEY (fromId, toId),"
                + " FOREIGN KEY(fromId) REFERENCES artifacts(id) ON DELETE CASCADE,"
                + " FOREIGN KEY(toId) REFERENCES artifacts(id) ON DELETE CASCADE"
                + ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createArtifactsTable);
            stmt.execute(createLinksTable);
            logger.info("Khởi tạo bảng 'artifacts' và 'links' thành công.");
        }
    }

    @Override
    public void clearIndex() throws SQLException {
        String deleteLinks = "DELETE FROM links;";
        String deleteArtifacts = "DELETE FROM artifacts;";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(deleteLinks);
            stmt.execute(deleteArtifacts);
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

        String sql = "INSERT OR REPLACE INTO artifacts (id, name, type, status) VALUES(?,?,?,?);";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artifact.getId());
            pstmt.setString(2, artifact.getName());
            pstmt.setString(3, artifact.getArtifactType());
            pstmt.setString(4, status);
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
        String sql = "SELECT id, name, type FROM artifacts "
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
                results.add(artifact);
            }
        }
        return results;
    }

    @Override
    public List<Artifact> queryBacklinks(String artifactId) throws SQLException {
        List<Artifact> results = new ArrayList<>();
        String sql = "SELECT a.id, a.name, a.type FROM artifacts a "
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
                results.add(artifact);
            }
        }
        return results;
    }

    /**
     * [THÊM MỚI NGÀY 28] Triển khai logic query
     */
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

    /**
     * [THÊM MỚI NGÀY 28] Triển khai logic query
     */
    @Override
    public List<Artifact> getArtifactsByStatus(String status) throws SQLException {
        List<Artifact> results = new ArrayList<>();
        String sql = "SELECT id, name, type FROM artifacts WHERE status = ?;";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Artifact artifact = new Artifact();
                artifact.setId(rs.getString("id"));
                artifact.setName(rs.getString("name"));
                artifact.setArtifactType(rs.getString("type"));
                results.add(artifact);
            }
        }
        return results;
    }
}