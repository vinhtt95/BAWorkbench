package com.rms.app.service;

import com.rms.app.model.Artifact;

import java.io.IOException;

// Interface (Giao diện) - Tuân thủ DIP
public interface IArtifactRepository {

    /**
     * Lưu một đối tượng (Artifact) vào file system.
     * Sẽ triển khai dual-write (json + md) ở Giai đoạn 3.
     */
    void save(Artifact artifact) throws IOException;

    /**
     * Tải một đối tượng (Artifact) từ file system bằng ID.
     */
    Artifact load(String id) throws IOException;

    /**
     * Xóa một đối tượng (Artifact).
     */
    void delete(String id) throws IOException;
}