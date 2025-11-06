# Business Requirements Document (BRD)
# (Vision and Scope Document)
cho
Hệ thống Quản lý Yêu cầu (Requirements Management System - RMS)
Phiên bản 1.0

Ngày 06 tháng 11 năm 2025

---

## 1. Business Requirements (Yêu cầu Nghiệp vụ)

### 1.1. Background (Bối cảnh)
Hiện tại, các Chuyên gia Phân tích Nghiệp vụ (BA) đang quản lý các yêu vầu dự án (requirements artifacts) bằng một bộ công cụ rời rạc:
* Đặc tả yêu cầu (SRS, Use Cases) được viết bằng **Microsoft Word**.
* Danh sách tính năng, Business Rules, và Ma trận Truy vết (RTM) được quản lý bằng **Microsoft Excel**.
* Quy trình nghiệp vụ (Workflows), Sơ đồ Hoạt động, BPMN được vẽ bằng các công cụ bên ngoài (ví dụ: **Visio, Draw.io, Figma**).

### 1.2. Business Problem (Vấn đề Nghiệp vụ)
Quy trình thủ công và phân mảnh này dẫn đến các vấn đề nghiêm trọng:
* **Thiếu sự đồng nhất (Lack of Consistency):** Dữ liệu bị trùng lặp và không nhất quán giữa các file. Một thay đổi ở file Word không được tự động cập nhật trong file Excel hoặc Visio.
* **Mất khả năng Truy vết (Loss of Traceability):** Rất khó để theo dõi mối quan hệ giữa một Business Rule (trong Excel) với một Use Case (trong Word) và một Sơ đồ (trong Visio).
* **Khó bảo trì (High Maintenance):** Việc cập nhật (change request) rất tốn thời gian và dễ gây lỗi, vì BA phải cập nhật thủ công ở nhiều nơi.
* **Khó trực quan hóa (Poor Visualization):** BA không có "bức tranh lớn" (big picture) về sự phụ thuộc giữa các yêu cầu. Họ chỉ thấy "một đống text".

### 1.3. Business Opportunity (Cơ hội Nghiệp vụ)
Chúng ta có cơ hội xây dựng một công cụ desktop duy nhất, tích hợp (all-in-one) để giải quyết tất cả các vấn đề trên. Bằng cách quản lý các đối tượng yêu cầu (artifacts) như các "đối tượng" (objects) có cấu trúc và quan hệ (thay vì các file tĩnh), chúng ta có thể:
* Tạo ra một **Nguồn sự thật Duy nhất (Single Source of Truth)**.
* **Tự động hóa** việc tạo sơ đồ từ đặc tả.
* **Tự động hóa** việc truy vết (traceability).
* Giảm đáng kể thời gian và lỗi trong việc quản lý yêu cầu.

---

## 2. Vision of the Solution (Tầm nhìn Giải pháp)

### 2.1. Vision Statement (Tuyên bố Tầm nhìn)
Cung cấp cho các Business Analyst (BA) một **Hệ thống Quản lý Yêu cầu (RMS)** thế hệ mới, hoạt động trên desktop (local-first). Hệ thống này biến việc soạn thảo tài liệu yêu cầu từ một công việc "viết văn bản" (text-editing) thành một quy trình **"mô hình hóa nghiệp vụ" (business-modeling)** trực quan, không cần viết code (no-code), nơi các sơ đồ (diagrams) được tự động sinh ra từ dữ liệu nhập trên Form.

### 2.2. Major Features (Các tính năng chính)
Hệ thống sẽ cung cấp 6 gói tính năng chính:
* **F-CFG (Cấu hình):** Cho phép BA tự thiết kế các Form nhập liệu (`Form Builder`) và các Template xuất bản (`Export Template Builder`) mà không cần viết code.
* **F-DEV (Phát triển):** Cho phép BA nhập liệu yêu cầu (Use Cases, Rules...) thông qua các Form trực quan, bao gồm một `Flow Builder` (trình tạo luồng) kéo-thả.
* **F-MOD (Mô hình hóa):** Tự động sinh sơ đồ PlantUML (Sơ đồ Hoạt động) từ dữ liệu `Flow Builder`, và cung cấp các công cụ phân tích (Graph View, Backlinks).
* **F-MGT (Quản lý):** Cung cấp các công cụ quản lý tiến độ (Kanban/Gantt) và quản lý phạm vi (Release Versioning).
* **F-PM (Quản lý Dự án):** Cho phép tạo/mở dự án và import/export dữ liệu thô (Excel).
* **F-PUB (Xuất bản):** Cho phép xuất bản các tài liệu SRS (PDF/DOCX) chuyên nghiệp dựa trên các template tùy chỉnh.

---

## 3. Scope and Limitations (Phạm vi và Giới hạn)

### 3.1. Scope of Initial Release (Phạm vi v1.0)
Phiên bản 1.0 sẽ tập trung vào trải nghiệm của BA cá nhân (single-user):
* Ứng dụng **Desktop** (JavaFX) chạy trên Windows, macOS, Linux.
* Lưu trữ **Local-first** (dữ liệu là file `.json` và `.md` trên ổ đĩa của người dùng).
* Tương thích 100% với **Git** (nhờ cơ chế dual-write .json/.md).
* Hỗ trợ "No-code" (Form-driven) cho nhập liệu.
* Tự động sinh sơ đồ (PlantUML) từ dữ liệu Form.
* Tích hợp API (Gemini, Figma-link, Pandoc, Apache POI).
* Quản lý tiến độ và phiên bản cơ bản.
* Xuất bản tài liệu mạnh mẽ.

### 3.2. Limitations and Exclusions (Giới hạn và Loại trừ)
Phiên bản 1.0 sẽ **KHÔNG** bao gồm:
* **Phiên bản Web/Cloud:** Đây hoàn toàn là ứng dụng desktop.
* **Hợp tác thời gian thực (Real-time Collaboration):** Không hỗ trợ nhiều người dùng chỉnh sửa cùng lúc (việc đồng bộ sẽ dựa vào Git).
* **Cơ sở dữ liệu tập trung (Centralized DB):** Không sử dụng SQL hay NoSQL. Nguồn sự thật là các file trên ổ đĩa.
* **Tích hợp 2 chiều (2-way sync):** Không tự động đồng bộ 2 chiều với JIRA, DevOps (chỉ xuất bản tài liệu cho họ đọc).

---

## 4. Business Context (Bối cảnh Nghiệp vụ)

### 4.1. Stakeholder Profiles (Các bên liên quan)

| Stakeholder | Vai trò | Mối quan tâm (Interests) |
| :--- | :--- | :--- |
| **Business Analyst (BA)** | (Người dùng chính) | - Giảm thời gian nhập liệu thủ công.<br>- Không muốn học code (PlantUML, Mermaid).<br>- Muốn tài liệu và sơ đồ luôn đồng nhất.<br>- Muốn truy vết (traceability) dễ dàng. |
| **Project Manager (PM)** | (Người dùng phụ) | - Muốn biết tiến độ hoàn thành tài liệu (Kanban/Gantt).<br>- Muốn biết phạm vi (scope) của từng phiên bản (Release).<br>- Muốn có tài liệu SRS chuẩn hóa để chia sẻ. |
| **Developer / QA Team** | (Người tiêu thụ) | - Muốn một tài liệu SRS (PDF/DOCX) rõ ràng, duy nhất, không mâu thuẫn để đọc và code/test. |

### 4.2. Business Risks (Rủi ro Nghiệp vụ)
| ID Rủi ro | Rủi ro | Mức độ | Kế hoạch Giảm thiểu |
| :--- | :--- | :--- | :--- |
| **R-01** | BA không chấp nhận (Low Adoption) | Trung bình | Đảm bảo triết lý "No-code" (đặc biệt là `Flow Builder` và `Form Builder`) phải thật sự trực quan và dễ sử dụng hơn cách làm cũ (Word/Excel). |
| **R-02** | Phụ thuộc Công cụ Bên ngoài | Thấp | Phụ thuộc vào **Pandoc** để xuất PDF/DOCX. Phải thông báo rõ ràng cho người dùng yêu cầu cài đặt Pandoc, và cung cấp hướng dẫn cài đặt. |
| **R-03** | Hiệu suất kém (Performance) | Cao | Với các dự án lớn (hàng ngàn đối tượng), việc quét file, tạo Graph View, và Autocomplete (`@ID`) có thể bị chậm. Kiến trúc (`SearchService`) phải sử dụng cơ chế cache và chỉ mục (indexing) hiệu quả. |