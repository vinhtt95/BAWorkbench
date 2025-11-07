package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.service.IArtifactRepository;
import com.rms.app.service.ISearchService;
import com.rms.app.service.ISqliteIndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai ISearchService.
 * Dịch vụ này hiện truy vấn trực tiếp CSDL Chỉ mục (SQLite).
 * [CẬP NHẬT] Thêm phương thức lấy cây (tree)
 */
public class SearchServiceImpl implements ISearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    private final ISqliteIndexRepository indexRepository;

    /**
     * Inject IArtifactRepository để hỗ trợ việc Load/Save (Triple-Write)
     * khi cập nhật trạng thái từ Kanban.
     */
    private final IArtifactRepository artifactRepository;

    @Inject
    public SearchServiceImpl(ISqliteIndexRepository indexRepository, IArtifactRepository artifactRepository) {
        this.indexRepository = indexRepository;
        this.artifactRepository = artifactRepository;
    }

    /**
     * Hàm này không còn được sử dụng vì IndexServiceImpl
     * chịu trách nhiệm lập chỉ mục.
     *
     * @throws IOException Nếu lỗi I/O
     */
    @Override
    public void buildIndex() throws IOException {
        logger.info("SearchServiceImpl.buildIndex() không còn được sử dụng. IndexServiceImpl đang xử lý.");
    }

    /**
     * Tìm kiếm CSDL chỉ mục (SQLite) cho autocomplete.
     * Tham chiếu (F-DEV-06)
     *
     * @param query Từ khóa
     * @return Danh sách Artifacts
     */
    @Override
    public List<Artifact> search(String query) {
        try {
            return indexRepository.queryArtifacts(query);
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi tìm kiếm autocomplete cho '{}': {}", query, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Tìm kiếm CSDL chỉ mục (SQLite) cho backlinks.
     * Tham chiếu (F-MOD-03)
     *
     * @param artifactId ID của Artifact
     * @return Danh sách Artifacts
     */
    @Override
    public List<Artifact> getBacklinks(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return indexRepository.queryBacklinks(artifactId);
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi truy vấn backlinks cho '{}': {}", artifactId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Triển khai logic nghiệp vụ cho Kanban (UC-MGT-02).
     *
     * @return Map (Ánh xạ) {Status -> List<Artifact>}
     */
    @Override
    public Map<String, List<Artifact>> getArtifactsGroupedByStatus() {
        Map<String, List<Artifact>> resultMap = new HashMap<>();
        try {
            /**
             * 1. Lấy tất cả các cột (Status)
             */
            List<String> statuses = indexRepository.getDefinedStatuses();

            /**
             * 2. Với mỗi cột, lấy các artifact (thẻ)
             */
            for (String status : statuses) {
                List<Artifact> artifacts = indexRepository.getArtifactsByStatus(status);
                resultMap.put(status, artifacts);
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi nhóm (grouping) artifacts cho Kanban: {}", e.getMessage());
        }
        return resultMap;
    }

    /**
     * [MỚI] Triển khai logic nghiệp vụ cho TreeView
     *
     * @return Map (Ánh xạ) {Type -> List<Artifact>}
     */
    @Override
    public Map<String, List<Artifact>> getArtifactsGroupedByType() {
        Map<String, List<Artifact>> resultMap = new HashMap<>();
        try {
            /**
             * 1. Lấy tất cả các Loại (Type)
             */
            List<String> types = indexRepository.getDefinedTypes();

            /**
             * 2. Với mỗi Loại, lấy các artifact (lá)
             */
            for (String type : types) {
                List<Artifact> artifacts = indexRepository.getArtifactsByType(type);
                resultMap.put(type, artifacts);
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi nhóm (grouping) artifacts cho TreeView: {}", e.getMessage());
        }
        return resultMap;
    }


    /**
     * Triển khai logic cập nhật trạng thái (F-MGT-03).
     *
     * @param artifact   Đối tượng (chỉ chứa ID, Type) được kéo
     * @param newStatus  Trạng thái (Status) mới (tên của cột được thả vào)
     * @throws IOException Nếu lỗi load hoặc save (Triple-Write)
     */
    @Override
    public void updateArtifactStatus(Artifact artifact, String newStatus) throws IOException {
        if (artifact == null || newStatus == null) {
            throw new IOException("Artifact hoặc Trạng thái mới không được null.");
        }

        /**
         * 1. Xác định đường dẫn tương đối (Relative Path)
         */
        if (artifact.getArtifactType() == null) {
            throw new IOException("ArtifactType là null, không thể xác định đường dẫn file.");
        }
        String relativePath = artifact.getArtifactType() + File.separator + artifact.getId() + ".json";
        logger.info("Đang cập nhật trạng thái cho: {}", relativePath);

        /**
         * 2. Tải (Load) toàn bộ artifact (Source of Truth)
         */
        Artifact fullArtifact = artifactRepository.load(relativePath);

        /**
         * 3. Thay đổi (Mutate) trạng thái
         */
        fullArtifact.getFields().put("Trạng thái", newStatus);

        /**
         * 4. Lưu (Save) (Kích hoạt Triple-Write)
         */
        artifactRepository.save(fullArtifact);
        logger.info("Đã cập nhật trạng thái của {} thành {}", artifact.getId(), newStatus);
    }

    /**
     * [THÊM MỚI] Triển khai (implementation)
     * logic lấy (fetch) dữ liệu (data) đồ thị (UC-MOD-02).
     *
     * @return Map (Ánh xạ) chứa "nodes" và "edges"
     * @throws IOException Nếu lỗi CSDL (SQL)
     */
    @Override
    public Map<String, List<Map<String, String>>> getGraphData() throws IOException {
        try {
            Map<String, List<Map<String, String>>> data = new HashMap<>();
            data.put("nodes", indexRepository.getAllNodes());
            data.put("edges", indexRepository.getAllEdges());
            return data;
        } catch (SQLException e) {
            logger.error("Lỗi SQL nghiêm trọng khi lấy (fetch) dữ liệu đồ thị (graph)", e);
            throw new IOException("Không thể tải (load) dữ liệu Sơ đồ Quan hệ. Lỗi CSDL.", e);
        }
    }
}