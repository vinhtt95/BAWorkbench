package com.rms.app.service;

import com.rms.app.model.Artifact;

import java.io.IOException;

/**
 * Interface (Giao diện) - Tuân thủ DIP
 */
public interface IArtifactRepository {

    /**
     * Lưu một đối tượng (Artifact) vào file system.
     * Sẽ triển khai dual-write (json + md) ở Giai đoạn 3.
     *
     * @param artifact Đối tượng (Artifact) cần lưu
     * @throws IOException Nếu lỗi ghi file
     */
    void save(Artifact artifact) throws IOException;

    /**
     * Tải một đối tượng (Artifact) từ file system bằng đường dẫn tương đối.
     *
     * @param relativePath Đường dẫn tương đối (ví dụ: "UC/UC001.json")
     * @return Artifact đã tải
     * @throws IOException Nếu không tìm thấy file
     */
    Artifact load(String relativePath) throws IOException;

    /**
     * Xóa một đối tượng (Artifact) bằng đường dẫn tương đối.
     *
     * @param relativePath Đường dẫn tương đối (ví dụ: "UC/UC001.json")
     * @throws IOException Nếu lỗi xóa file
     */
    void delete(String relativePath) throws IOException;
}