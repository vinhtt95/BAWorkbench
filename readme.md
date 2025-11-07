# BA Workbench (Hệ thống Quản lý Yêu cầu)

**BA Workbench** là một ứng dụng desktop, ưu tiên lưu trữ local (local-first) được xây dựng cho các Chuyên gia Phân tích Nghiệp vụ (BA) để quản lý yêu cầu.

Nó thay thế quy trình làm việc thủ công (Word + Excel + Visio) bằng một "Nguồn sự thật" (Single Source of Truth) duy nhất. Ứng dụng cho phép bạn định nghĩa các loại yêu cầu (ví dụ: Use Case, Business Rule) bằng trình tạo form "No-code", nhập liệu bằng form động, và tự động sinh sơ đồ (PlantUML) cũng như các tài liệu SRS (PDF/DOCX) chuyên nghiệp.

## Các Tính năng Chính

* **Trình thiết kế Form (F-CFG):** Tùy chỉnh hoàn toàn các loại artifact và các trường dữ liệu (field) của bạn (ví dụ: `Text`, `Dropdown`, `Flow Builder`, `Figma Link`).
* **Phát triển Yêu cầu (F-DEV):** Nhập liệu dựa trên form, tự động lưu (auto-save), và tự động tạo file `.md` (Git-friendly mirror).
* **Tích hợp AI (F-DEV):** Sử dụng Gemini để đề xuất các bước (step) cho `Flow Builder`.
* **Mô hình hóa (F-MOD):** Tự động sinh Sơ đồ Hoạt động (PlantUML) từ `Flow Builder` và xem Sơ đồ Quan hệ (Graph View) của toàn bộ dự án.
* **Quản lý (F-MGT):** Theo dõi tiến độ với bảng Kanban (kéo-thả) và quản lý phiên bản (Releases).
* **Xuất bản (F-PUB):** Xuất (Export) dữ liệu ra Excel hoặc tạo tài liệu PDF/DOCX tùy chỉnh bằng Trình thiết kế Template Xuất bản.

## Yêu cầu Hệ thống (Bắt buộc)

Bạn **phải** cài đặt các công cụ sau và thêm chúng vào `PATH` hệ thống để ứng dụng hoạt động đầy đủ.

1.  **Java JDK 21+:** Cần thiết để chạy ứng dụng.
2.  **Pandoc:** Cần thiết cho chức năng "Export to Document (PDF/DOCX)" (UC-PUB-01).
3.  **XeLaTeX (Khuyến nghị: BasicTeX hoặc MiKTeX):** Cần thiết cho Pandoc để tạo file PDF hỗ trợ Unicode (Tiếng Việt).

## Cách Chạy (Development)

1.  Clone (Sao chép) repository.
2.  Mở dự án trong IntelliJ IDEA hoặc IDE Java bất kỳ.
3.  Chạy lệnh Maven:
    ```bash
    mvn clean javafx:run
    ```

## Cách Đóng gói (Tạo file cài đặt)

Ứng dụng này sử dụng `jpackage` (thông qua Maven) để tạo file cài đặt gốc (`.exe`, `.dmg`, `.deb`).

1.  Đảm bảo các yêu cầu hệ thống (Java, Pandoc, XeLaTeX) đã được cài đặt.
2.  Chạy lệnh Maven sau:
    ```bash
    mvn clean package
    ```
3.  Tìm file cài đặt của bạn trong thư mục `target/jpackage/`.