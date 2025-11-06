package com.rms.app.service;

import com.rms.app.model.Artifact;

/**
 * Interface (cho DIP) của Service Lập Chỉ mục.
 * Chịu trách nhiệm điều phối (orchestrate) việc xây dựng CSDL Chỉ mục.
 * Tham chiếu Kế hoạch Ngày 19 (Giai đoạn 4).
 * [vinhtt95/baworkbench/BAWorkbench-8718cda1cf4b17075f34bedd73e1e9e86e29ecc3/Requirement/ImplementPlan.md]
 */
public interface IIndexService {

    /**
     * Quét toàn bộ thư mục Artifacts/, phân tích các file .json (Source of Truth),
     * và xây dựng lại toàn bộ CSDL Chỉ mục (index.db) từ đầu.
     * PHẢI chạy trên luồng nền (background thread).
     * Tham chiếu UC-PM-04 [vinhtt95/baworkbench/BAWorkbench-8718cda1cf4b17075f34bedd73e1e9e86e29ecc3/Requirement/UseCases/UC-PM-04.md]
     */
    void validateAndRebuildIndex();

    /**
     * Cập nhật (Upsert) một artifact duy nhất vào chỉ mục.
     * (Sẽ dùng ở Ngày 20)
     * @param artifact Đối tượng cần cập nhật
     */
    void updateArtifactInIndex(Artifact artifact);

    /**
     * Xóa một artifact khỏi chỉ mục.
     * (Sẽ dùng ở Ngày 20)
     * @param artifactId ID của đối tượng cần xóa
     */
    void deleteArtifactFromIndex(String artifactId);

    /**
     * Kiểm tra xem một artifact có liên kết ngược hay không.
     * (Sẽ dùng ở Ngày 20)
     * @param artifactId ID của đối tượng cần kiểm tra
     * @return true nếu có liên kết ngược, ngược lại false
     */
    boolean hasBacklinks(String artifactId);

}