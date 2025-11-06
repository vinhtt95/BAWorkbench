# Software Requirements Specification
cho
Hệ thống Quản lý Yêu cầu (Requirements Management System - RMS)
Phiên bản 1.0 (Cập nhật SQLite)

Ngày 06 tháng 11 năm 2025

# Table of Contents
(Nội dung Table of Contents không thay đổi)

---

# 1. Introduction
(Không thay đổi)

---

# 2. Overall Description

## 2.1. Product Perspective
Hệ thống RMS là một sản phẩm phần mềm mới, độc lập (standalone). Nó được thiết kế để trở thành công cụ chính trong bộ công cụ (toolchain) của BA. RMS hoạt động dựa trên một kiến trúc "Hybrid":
1.  **Hệ thống file local (File-based):** Dữ liệu "Source of Truth" (`.json`) và "Git-friendly mirror" (`.md`) được lưu trên ổ đĩa.
2.  **Cơ sở dữ liệu Chỉ mục (Index Database):** Một file SQLite (`index.db`) được sử dụng để tăng tốc độ truy vấn và đảm bảo tính toàn vẹn của liên kết.
    Hệ thống 100% tương thích với các hệ thống Quản lý Phiên bản (VCS) như Git (vì Git chỉ theo dõi các file `.json` và `.md`).

## 2.2. Product Features
(Không thay đổi - Danh sách các gói F-PM, F-CFG...)

## 2.3. User Classes and Characteristics
(Không thay đổi)

## 2.4. Operating Environment
* **Hệ điều hành:** Đa nền tảng (Windows 10/11, macOS 13+, Linux).
* **Runtime:** Java Runtime Environment (JRE/JDK) 17+ và JavaFX Runtime 17+.
* **Database Engine:** SQLite (thư viện `sqlite-jdbc` được nhúng).
* **Tài nguyên:** Tối thiểu 4GB RAM, 500MB dung lượng ổ đĩa.

## 2.5. Design and Implementation Constraints
* **C-01 (Ngôn ngữ):** Hệ thống PHẢI được phát triển bằng ngôn ngữ Java (JDK 17+).
* **C-02 (UI Framework):** Giao diện người dùng PHẢI được xây dựng bằng JavaFX.
* **C-03 (Kiến trúc):** PHẢI tuân thủ chặt chẽ mẫu kiến trúc MVVM và các nguyên tắc SOLID.
* **C-04 (Lưu trữ):** Dữ liệu dự án PHẢI được lưu trữ trên hệ thống file local (Local-first).
* **C-05 (Định dạng "Source of Truth"):** Dữ liệu nhập từ Form PHẢI được lưu dưới dạng file `.json` (hoặc `.yaml`). Đây là nguồn sự thật (Source of Truth).
* **C-06 (Định dạng Git-Friendly):** Một file `.md` (Markdown) PHẢI được tự động sinh ra (auto-generate) từ file `.json` trên mỗi lần lưu, chỉ nhằm mục đích cung cấp "diff" rõ ràng cho Git.
* **C-07 (Tương tác):** Mọi thao tác cấu hình (Form Builder, Export Template) PHẢI thực hiện qua GUI (No-code).
* **C-08 (Thư viện Sơ đồ):** PHẢI sử dụng thư viện PlantUML (bản Java `.jar`) để render sơ đồ.
* **C-09 (Thư viện Xuất bản):** PHẢI dựa vào công cụ **Pandoc** (cài đặt bên ngoài) để chuyển đổi từ Markdown trung gian sang PDF/DOCX.
* **C-10 (Mới - Lớp Chỉ mục):** PHẢI sử dụng một cơ sở dữ liệu **SQLite** (`index.db`) làm "Lớp Chỉ mục và Đảm bảo Toàn vẹn". Lớp này dùng để tăng tốc truy vấn (Autocomplete, Backlinks, Kanban) và kiểm tra (check) liên kết trước khi xóa.
* **C-11 (Mới - Tương thích Git):** File chỉ mục (`index.db`) và các file cache (`.cache/`) PHẢI được tự động thêm vào file `.gitignore` của dự án khi tạo dự án (UC-PM-01). Chúng không phải là "Source of Truth" và không được commit lên Git.

## 2.6. Assumptions and Dependencies
* **A-01:** Người dùng (BA) có kiến thức cơ bản về Git để quản lý phiên bản thư mục dự án (quản lý các file `.json` và `.md`).
* **A-02:** Người dùng đã cài đặt **Pandoc** và cấu hình nó trong PATH hệ thống để sử dụng chức năng Xuất bản (UC-PUB-01).
* **A-03:** Người dùng có quyền đọc/ghi trên thư mục mà họ chọn để tạo/mở dự án.
* **A-04:** Người dùng có kết nối Internet đang hoạt động để sử dụng các tính năng API (Gemini, Figma).

---

# 3. System Features (Functional Requirements)

*Phần này tham chiếu đến `Functional Requirements Document.md` (FRD) đã được cập nhật ở trên. Các yêu cầu (F-PM-02, F-PM-05, F-DEV-05, F-DEV-06, F-DEV-10, F-DEV-11, F-MOD-03, F-MOD-04, F-MGT-02, F-MGT-04) đã được cập nhật để phản ánh kiến trúc SQLite.*

---

# 4. External Interface Requirements

## 4.1. User Interfaces
(Không thay đổi - Bố cục 3 cột, Dark Mode, No-Code...)

## 4.2. Hardware Interfaces
* Không có yêu cầu giao diện phần cứng đặc biệt.

## 4.3. Software Interfaces
* **SI-01 (File System):** Hệ thống PHẢI sử dụng `java.nio.file` để đọc/ghi file `.json` và `.md`.
* **SI-02 (Diagramming):** Hệ thống PHẢI tích hợp thư viện **PlantUML** (dạng `.jar`).
* **SI-03 (Document Conversion):** Hệ thống PHẢI gọi (execute command) công cụ **Pandoc** được cài đặt bên ngoài.
* **SI-04 (Excel Handling):** Hệ thống PHẢI tích hợp thư viện (ví dụ: Apache POI).
* **SI-05 (Data Serialization):** Hệ thống PHẢI tích hợp thư viện (ví dụ: Jackson) để serialize/deserialize file `.json`.
* **SI-06 (Mới - Database):** Hệ thống PHẢI tích hợp thư viện **`sqlite-jdbc`** để đọc/ghi vào file chỉ mục `index.db`.

## 4.4. Communications Interfaces
* **CI-01 (HTTPS):** Hệ thống PHẢI sử dụng `java.net.http.HttpClient` để giao tiếp với các API của bên thứ ba (Gemini, Figma).

---

# 5. Nonfunctional Requirements

## 5.1. Performance Requirements
* **PERF-01 (Auto-save):** Cơ chế auto-save (Triple-write: JSON, MD, SQLite) PHẢI hoàn tất trong vòng 2 giây và chạy ngầm (asynchronously).
* **PERF-02 (Form Rendering):** "Form View" PHẢI được render < 500ms.
* **PERF-03 (Diagram Rendering):** "Diagram View" PHẢI được render < 1 giây.
* **PERF-04 (Autocomplete & Backlinks):** Các truy vấn liên kết (từ `F-DEV-06`, `F-MOD-03`) PHẢI trả về kết quả < 300ms (nhờ truy vấn vào Chỉ mục SQLite).
* **PERF-05 (Mới - Indexing):** Quá trình "Quét và Lập Chỉ mục" (F-PM-02) khi mở dự án (ví dụ: 1000 artifacts) PHẢI chạy hoàn toàn ở chế độ nền (background thread) và không "block" (chặn) UI.

## 5.2. Security Requirements
* **SEC-01 (API Keys):** Các API Key (Gemini, Figma) PHẢI được lưu trữ an toàn, KHÔNG được lưu trong thư mục dự án (để tránh commit lên Git).

## 5.3. Software Quality Attributes
* **QA-01 (Maintainability):** Code PHẢI tuân thủ MVVM, SOLID, và Dependency Injection (Guice).
* **QA-02 (Usability):** Toàn bộ hệ thống PHẢI hoạt động theo triết lý "No-code".
* **QA-03 (Reliability):** Cơ chế auto-save (PERF-01) PHẢI được bật mặc định.
* **QA-04 (Portability):** Hệ thống PHẢI chạy nhất quán trên Windows, macOS, Linux.
* **QA-05 (Mới - Data Integrity):** Hệ thống PHẢI cung cấp các cơ chế (F-DEV-10, F-PM-05) để đảm bảo tính toàn vẹn của các liên kết (`@ID`) trong dự án.

---

# 6. Other Requirements

* **OTH-01 (Data Synchronization):** Trách nhiệm đồng bộ (sync) file (ví dụ: qua Git, Dropbox) thuộc về người dùng.
* **OTH-02 (Data Architecture - Cập nhật):** Hệ thống sử dụng kiến trúc dữ liệu 3 thành phần:
    1.  **Source of Truth (File `.json`):** Dữ liệu thô từ Form. Đây là dữ liệu được Git theo dõi.
    2.  **Git Mirror (File `.md`):** Được auto-generate từ `.json`. Dùng để "diff" trên Git.
    3.  **Index Database (File `index.db`):** Được auto-generate từ `.json`. Dùng để tăng tốc truy vấn và đảm bảo toàn vẹn. File này **KHÔNG** được Git theo dõi.

---

# Appendix A: Glossary
(Thêm thuật ngữ mới)
...
| Thuật ngữ | Định nghĩa |
| :--- | :--- |
| **Index DB (Chỉ mục)** | (Mới) Một file SQLite (`index.db`) chứa bản đồ (map) của tất cả các đối tượng và liên kết trong dự án. Dùng để tăng tốc độ và đảm bảo toàn vẹn. File này là "disposable" (có thể vứt bỏ). |
| **Source of Truth (SoT)** | (Mới) Nguồn sự thật. Trong kiến trúc này, đó là các file `.json`. Nếu `index.db` và `.json` xung đột, `.json` luôn thắng. |
...

# Appendix B: Analysis Models (Use Cases)
*Toàn bộ các Use Case chi tiết (đã được tạo ở các bước trước) sẽ được đưa vào đây, hoặc tham chiếu đến thư mục `UseCases/`.*
* ...
* (Mới) `UseCases/UC-PM-04.md` (Kiểm tra & Tái lập Chỉ mục)