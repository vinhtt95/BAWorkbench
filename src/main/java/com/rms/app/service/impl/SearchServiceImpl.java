package com.rms.app.service.impl;

import com.google.inject.Inject;
import com.rms.app.model.Artifact;
import com.rms.app.service.ISearchService;
import com.rms.app.service.ISqliteIndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * [TÁI CẤU TRÚC NGÀY 21 & 22]
 * Triển khai ISearchService.
 * Dịch vụ này hiện truy vấn trực tiếp CSDL Chỉ mục (SQLite) thay vì
 * duy trì một cache riêng trong bộ nhớ.
 * Tham chiếu Kế hoạch [vinhtt95/baworkbench/BAWorkbench-b81f6c2eab10596eeb739d9111f5ef0610b2666e/Requirement/ImplementPlan.md]
 */
public class SearchServiceImpl implements ISearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    private final ISqliteIndexRepository indexRepository;

    @Inject
    public SearchServiceImpl(ISqliteIndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    /**
     * Hàm này không còn được sử dụng vì IndexServiceImpl
     * chịu trách nhiệm lập chỉ mục.
     */
    @Override
    public void buildIndex() throws IOException {
        logger.info("SearchServiceImpl.buildIndex() không còn được sử dụng. IndexServiceImpl đang xử lý.");
    }

    /**
     * [TÁI CẤU TRÚC NGÀY 21]
     * Tìm kiếm CSDL chỉ mục (SQLite) cho autocomplete.
     * Tham chiếu (F-DEV-06) [vinhtt95/baworkbench/BAWorkbench-b81f6c2eab10596eeb739d9111f5ef0610b2666e/Requirement/Functional Requirements Document.md]
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
     * [TÁI CẤU TRÚC NGÀY 22]
     * Tìm kiếm CSDL chỉ mục (SQLite) cho backlinks.
     * Tham chiếu (F-MOD-03) [vinhtt95/baworkbench/BAWorkbench-b81f6c2eab10596eeb739d9111f5ef0610b2666e/Requirement/Functional Requirements Document.md]
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
}