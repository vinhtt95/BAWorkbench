# Functional Requirements Document (FRD)
# Hệ thống Quản lý Yêu cầu (RMS) v1.0

| | |
| :--- | :--- |
| **Dự án:** | Hệ thống Quản lý Yêu cầu (RMS) |
| **Phiên bản:** | 1.0 |
| **Ngày:** | 06/11/2025 |
| **Trạng thái:** | Đã cập nhật (SQLite Index) |

---

## 1. Giới thiệu

### 1.1. Mục đích
Tài liệu này đặc tả các Yêu cầu Chức năng (Functional Requirements - FR) cho Hệ thống Quản lý Yêu cầu (RMS) phiên bản 1.0.

### 1.2. Phạm vi (Scope)
(Không thay đổi)

### 1.3. Lớp người dùng
(Không thay đổi)

---

## 2. Đặc tả Yêu cầu Chức năng (Functional Requirements)

### 2.1. Gói Quản lý Dự án (Feature ID: F-PM)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-PM-01** | Tạo Dự án Mới | Hệ thống phải cho phép BA tạo một dự án mới. Hành động này sẽ tạo ra một cấu trúc thư mục chuẩn (ví dụ: `Artifacts/`, `.config/`). | UC-PM-01 |
| **F-PM-02** | Mở Dự án (Cập nhật)| Hệ thống phải cho phép BA mở một dự án đã tồn tại. Khi mở, hệ thống phải **tự động kích hoạt (ở chế độ nền)** quy trình "Quét và Lập Chỉ mục" (Scan & Index) để xây dựng/cập nhật cơ sở dữ liệu chỉ mục (SQLite). | UC-PM-02 |
| **F-PM-03** | Import từ Excel | Hệ thống phải cung cấp chức năng "Import from Excel" để cho phép BA "đổ" dữ liệu từ file `.xlsx` vào dự án. | UC-PM-03 |
| **F-PM-04** | Giao diện Ánh xạ Import | Khi import, hệ thống phải cung cấp một giao diện GUI (Import Wizard) cho phép BA ánh xạ (map) "Sheet" với "Loại Đối tượng" và "Cột" với "Trường". | UC-PM-03 |
| **F-PM-05** | (Mới) Kiểm tra & Tái lập Chỉ mục | Hệ thống phải cung cấp một chức năng (ví dụ: Menu "Dự án" -> "Kiểm tra & Tái lập Chỉ mục") cho phép BA chủ động chạy lại quy trình "Quét và Lập Chỉ mục" (F-PM-02) để đồng bộ hóa Chỉ mục (SQLite) với các file (`.json`). | UC-PM-04 (Mới) |

### 2.2. Gói Cấu hình Dự án (Feature ID: F-CFG)
(Không thay đổi so với file gốc)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-CFG-01** | Trình thiết kế Form (GUI) | (No-Code) Hệ thống phải cung cấp một giao diện GUI "Form Builder" (trong Settings) cho phép BA tạo/sửa/xóa các "Loại Đối tượng" (Artifact Types). | UC-CFG-01 |
| **F-CFG-02** | Các Loại trường (Fields) | "Form Builder" phải cung cấp một "Toolbox" các loại trường có thể kéo-thả, bao gồm: `Text`, `TextArea`, `Dropdown`, `Linker`, và `Flow Builder`. | UC-CFG-01 |
| **F-CFG-03** | Lưu Cấu hình Form | Khi BA "Save", hệ thống phải lưu cấu hình Form đó dưới dạng một file `.json` (hoặc `.yaml`) trong thư mục `.config`. | UC-CFG-01 |
| **F-CFG-04** | Quản lý Phiên bản (Release) | Hệ thống phải cung cấp một giao diện GUI (trong Settings) cho phép BA tạo/sửa/xóa các "Release" của dự án. | UC-CFG-02 |
| **F-CFG-05** | Trình thiết kế Template Xuất bản | (No-Code) Hệ thống phải cung cấp một giao diện GUI "Export Template Builder" (trong Settings) cho phép BA thiết kế cấu trúc file PDF/DOCX. | UC-CFG-03 |
| **F-CFG-06** | Chương Động (Dynamic Section) | "Export Template Builder" phải cho phép BA thêm "Chương Động", được điền dữ liệu tự động bằng "Trình tạo Truy vấn" (Query Builder) trực quan (ví dụ: "Lấy tất cả `@UC` có `Status=Approved`"). | UC-CFG-03 |
| **F-CFG-07** | Quản lý API Key | Hệ thống phải cung cấp giao diện GUI (trong Settings) để BA nhập và lưu trữ (một cách an toàn) các API Key (Gemini, Figma). | UC-CFG-04 (Mới) |

### 2.3. Gói Phát triển Yêu cầu (Feature ID: F-DEV)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-DEV-01** | Render Form Động | Khi BA tạo/sửa một đối tượng, hệ thống phải tự động sinh (render) ra "Form View" dựa trên template đã cấu hình. | UC-DEV-01 |
| **F-DEV-02** | Nhập liệu Form | "Form View" phải là giao diện **chính và duy nhất** để BA nhập và chỉnh sửa dữ liệu (Source of Truth). | UC-DEV-01 |
| **F-DEV-03** | Giao diện `Flow Builder` | (No-Code) Thành phần `Flow Builder` (trong Form) phải cung cấp một giao diện GUI cho phép BA định nghĩa logic quy trình (thêm bước, If-Then, kéo-thả). | UC-DEV-01 |
| **F-DEV-04** | Tự động lưu (Auto-save) | Hệ thống phải tự động lưu các thay đổi trên Form sau khi BA dừng nhập liệu (ví dụ: 2 giây). | UC-DEV-01 |
| **F-DEV-05** | Ghi đĩa ba (Triple-Write) (Cập nhật)| Trên mỗi lần lưu (Auto-save), hệ thống phải thực hiện 3 hành động: <br> 1. Ghi file `.json` (Source of Truth). <br> 2. Tự động tạo/cập nhật file `.md` (Git-friendly mirror). <br> 3. Tự động cập nhật (Upsert) dữ liệu của đối tượng đó trong Cơ sở dữ liệu Chỉ mục (SQLite). | C-05, C-06, C-10 (SRS) |
| **F-DEV-06** | Autocomplete Liên kết (Cập nhật)| Khi BA gõ `@`, hệ thống phải hiển thị một popup **autocomplete** cho phép tìm kiếm `@ID`. Popup này phải **đọc dữ liệu từ Chỉ mục SQLite** (để đảm bảo tốc độ). | UC-DEV-02 |
| **F-DEV-07** | Tương tác Liên kết | Hệ thống phải cung cấp 2 tương tác với tag `@ID`: **Hover** (hiển thị tóm tắt) và **Click** (mở Tab mới). | UC-DEV-02 |
| **F-DEV-08** | Tích hợp AI (Gemini) | Hệ thống phải cung cấp một nút "Gemini: Đề xuất" để **tự động điền dữ liệu** (ví dụ: các bước flow) vào Form. | UC-DEV-03 |
| **F-DEV-09** | Tích hợp Link Figma | Hệ thống phải cho phép BA dán (paste) một URL Figma vào một trường được chỉ định (hiển thị dưới dạng hyperlink). | UC-DEV-04 (Mới) |
| **F-DEV-10** | (Mới) Ngăn chặn Xóa (Toàn vẹn)| Khi BA thực hiện hành động "Xóa" một đối tượng (ví dụ: `@BR001`), hệ thống phải **truy vấn Chỉ mục SQLite** để kiểm tra liên kết ngược (Backlinks). | (Mới) |
| **F-DEV-11** | (Mới) Cảnh báo Xóa | (Tiếp theo F-DEV-10) Nếu đối tượng đang được liên kết bởi các đối tượng khác (ví dụ: `@UC001`), hệ thống phải **NGĂN CHẶN** hành động xóa và hiển thị cảnh báo: "Không thể xóa. `@BR001` đang được liên kết bởi: @UC001...". | (Mới) |

### 2.4. Gói Mô hình hóa Yêu cầu (Feature ID: F-MOD)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-MOD-01** | Tự động sinh Sơ đồ | Hệ thống phải cung cấp một tab "Diagram View" (Read-only) tự động sinh sơ đồ PlantUML từ dữ liệu đã nhập trong "Form View". | UC-MOD-01 |
| **F-MOD-02** | Tự động cập nhật Sơ đồ | Khi dữ liệu trong "Form View" được (Auto-save), "Diagram View" phải tự động làm mới (refresh) để phản ánh logic mới. | UC-MOD-01 |
| **F-MOD-03** | Xem Liên kết ngược (Cập nhật)| Hệ thống phải cung cấp một khu vực (ví dụ: Cột Phải) hiển thị "Backlinks". Dữ liệu này phải được **đọc từ Chỉ mục SQLite** (để đảm bảo tốc độ). | UC-MOD-03 |
| **F-MOD-04** | Xem Sơ đồ Quan hệ (Cập nhật)| Hệ thống phải cung cấp một "Project Graph View" (toàn cục) hiển thị tất cả các đối tượng (nodes) và liên kết (edges). Dữ liệu này phải được **đọc từ Chỉ mục SQLite**. | UC-MOD-02 |
| **F-MOD-05** | Sơ đồ Quy trình Tổng thể | Hệ thống phải hỗ trợ một "Loại Đối tượng" (`@BPMN`) cho phép BA tạo sơ đồ quy trình cấp cao bằng cách liên kết các `@UC` (tasks). Sơ đồ này phải hỗ trợ "drill-down". | UC-MOD-04 (Mới) |

### 2.5. Gói Quản lý Yêu cầu (Feature ID: F-MGT)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-MGT-01** | Quản lý Tác vụ | Hệ thống phải cho phép BA tạo và quản lý các đối tượng "Task" (`@TSK`) để theo dõi công việc. | UC-MGT-01 |
| **F-MGT-02** | Dashboard Kanban (Cập nhật)| Hệ thống phải cung cấp một "Kanban View". Dữ liệu (các thẻ và trạng thái) phải được **đọc từ Chỉ mục SQLite** (để đảm bảo tốc độ). | UC-MGT-02 |
| **F-MGT-03** | Cập nhật Kanban | BA phải có thể kéo-thả các thẻ trong "Kanban View" từ cột này sang cột khác. Hành động này phải **tự động cập nhật** trường "Status" của đối tượng (kích hoạt F-DEV-05). | UC-MGT-02 |
| **F-MGT-04** | Dashboard Gantt (Cập nhật)| Hệ thống phải cung cấp một "Gantt View". Dữ liệu (các `@TSK`) phải được **đọc từ Chỉ mục SQLite**. | UC-MGT-02 |
| **F-MGT-05** | Gán Phiên bản (Release) | BA phải có khả năng gán một Đối tượng Yêu cầu vào một "Release" thông qua một trường "Target Release" trong Form. | UC-MGT-03 |

### 2.6. Gói Xuất bản Tài liệu (Feature ID: F-PUB)
(Không thay đổi so với file gốc)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-PUB-01** | Xuất ra PDF/DOCX | Hệ thống phải cung cấp chức năng "Export to Document (PDF/DOCX)". | UC-PUB-01 |
| **F-PUB-02** | Lọc theo Phiên bản | Chức năng Export (F-PUB-01) phải cho phép BA **lọc** (filter) các yêu cầu sẽ được xuất bản dựa trên "Release". | UC-PUB-01 |
| **F-PUB-03** | Tuân thủ Template | Chức năng Export (F-PUB-01) phải gom (aggregate) nội dung và định dạng file đầu ra theo đúng cấu trúc đã thiết kế trong "Export Template" (từ `F-CFG-05`). | UC-PUB-01 |
| **F-PUB-04** | Xuất ra Excel | Hệ thống phải cung cấp chức năng "Export to Excel", tạo ra một file `.xlsx` trong đó mỗi "Loại Đối tượng" (`@UC`, `@BR`...) là một "Sheet" riêng biệt. | UC-PUB-02 |

---

## 3. Yêu cầu Giao diện (User Interface Requirements)
(Không thay đổi so với file gốc)

| ID | Yêu cầu | Mô tả chi tiết |
| :--- | :--- | :--- |
| **UI-01** | Bố cục IntelliJ | Giao diện phải tuân theo bố cục 3 cột: Cột Trái (Project TreeView), Cột Giữa (Main Work Area - TabPane), Cột Phải (Contextual Info). |
| **UI-02** | Đa cửa sổ | Hệ thống phải hỗ trợ BA kéo một `Tab` từ `TabPane` chính ra ngoài để trở thành một cửa sổ (Stage) riêng biệt. |
| **UI-03** | Theme (Chủ đề) | Hệ thống (v1.0) phải cung cấp một (và chỉ một) chủ đề "Dark Mode" (tối màu). |
| **UI-04** | Tùy chỉnh CSS | File CSS định nghĩa theme (UI-03) phải được lưu dưới dạng file bên ngoài (external file). |
| **UI-05** | Nguyên tắc "No-Code" | Tất cả các chức năng cấu hình (F-CFG) phải được thực hiện 100% qua giao diện GUI. BA không bao giờ bị bắt buộc phải sửa file `.json` hay viết code. |

---

## 4. Yêu cầu Phi chức năng (Non-Functional Requirements)

| ID | Yêu cầu | Mô tả chi tiết |
| :--- | :--- | :--- |
| **NFR-01** | Kiến trúc Lưu trữ (Cập nhật)| Hệ thống phải lưu trữ dữ liệu theo kiến trúc "Hybrid":<br>1. **Source of Truth:** Các file `.json` trên ổ đĩa.<br>2. **Index/Cache:** Một file **SQLite** (`.config/index.db`) để tăng tốc truy vấn và đảm bảo toàn vẹn. |
| **NFR-02** | Tương thích Git | Hệ thống phải tuân thủ kiến trúc "Dual-Write" (`.json` và `.md`) để đảm bảo mọi thay đổi trên *Source of Truth* đều có thể được "diff" bởi Git. |
| **NFR-03** | Tính toàn vẹn Chỉ mục (Mới)| File `index.db` (SQLite) phải được coi là **"disposable" (có thể vứt bỏ)**. Nó có thể được tạo lại hoàn toàn từ các file `.json` (Source of Truth) bất cứ lúc nào thông qua chức năng "Re-index" (F-PM-05). |
| **NFR-04** | Tương thích Git (Mới)| File `index.db` (SQLite) và các file nhị phân (binary) khác trong `.cache` **PHẢI** được thêm vào file `.gitignore` của dự án. |
| **NFR-05** | Tự động lưu (Reliability) | Hệ thống phải có cơ chế auto-save (kích hoạt sau 1-2 giây idle) để ngăn ngừa mất dữ liệu. |
| **NFR-06** | Hiệu suất (Performance) (Cập nhật)| <br> - Render Form (F-DEV-01): < 500ms. <br> - Render Sơ đồ (F-MOD-01): < 1 giây (cho luồng < 50 bước). <br> - Autocomplete (F-DEV-06): < 300ms (dựa vào truy vấn SQLite). <br> - Mở Dự án (F-PM-02): Quá trình Re-index (nếu cần) phải chạy nền (background) và không "block" UI. |
| **NFR-07** | Công nghệ (Constraints) | Phải được xây dựng bằng Java 17+ và JavaFX 17+. |
| **NFR-08** | Kiến trúc (Constraints) | Phải tuân thủ MVVM, SOLID, và sử dụng Dependency Injection (Guice). |
| **NFR-09** | Phụ thuộc (Constraints) (Cập nhật)| Phải sử dụng **SQLite-JDBC** (cho NFR-01), **PlantUML** (Java) cho (F-MOD-01) và **Pandoc** (CLI) cho (F-PUB-01). |

---

## 5. Tham chiếu
* `Software Requirement Specification.md`
* `Architecture Description.md`
* Thư mục `UseCases/` (chứa tất cả các file UC chi tiết)