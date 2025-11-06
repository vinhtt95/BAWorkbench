# Functional Requirements Document (FRD)
# Hệ thống Quản lý Yêu cầu (RMS) v1.0

| | |
| :--- | :--- |
| **Dự án:** | Hệ thống Quản lý Yêu cầu (RMS) |
| **Phiên bản:** | 1.0 |
| **Ngày:** | 06/11/2025 |
| **Trạng thái:** | Dự thảo |

---

## 1. Giới thiệu

### 1.1. Mục đích
Tài liệu này đặc tả các Yêu cầu Chức năng (Functional Requirements - FR) cho Hệ thống Quản lý Yêu cầu (RMS) phiên bản 1.0. Mục đích của tài liệu này là cung cấp một mô tả chi tiết về các chức năng, hành vi, và tương tác mà hệ thống phải cung cấp cho người dùng cuối (Business Analyst).

### 1.2. Phạm vi (Scope)
Hệ thống RMS là một ứng dụng desktop, "local-first" được thiết kế để trở thành công cụ làm việc chính của BA.

**Trong phạm vi (In-Scope):**
* Quản lý dự án (Tạo/Mở, Import/Export Excel).
* Cấu hình dự án (Tùy chỉnh "Loại Đối tượng" và "Template Xuất bản" qua GUI No-Code).
* Phát triển yêu cầu (Nhập liệu qua Form, `Flow Builder` trực quan).
* Truy vết yêu cầu (Tạo liên kết `@ID`, Backlinks, Graph View).
* Mô hình hóa (Tự động sinh sơ đồ PlantUML từ dữ liệu Form).
* Quản lý (Kanban, Gantt, Quản lý Release).
* Xuất bản (PDF/DOCX/Excel).

**Ngoài phạm vi (Out-of-Scope) cho v1.0:**
* Hợp tác thời gian thực (Real-time collaboration).
* Phiên bản Web/Cloud.
* Tích hợp tự động 2 chiều với JIRA/DevOps.

### 1.3. Lớp người dùng
* **Business Analyst (BA):** Người dùng chính. Thực hiện 90% các chức năng của hệ thống.
* **Project Manager (PM):** Người dùng phụ. Chủ yếu sử dụng các tính năng Quản lý (Dashboard) và Xuất bản (Đọc tài liệu).

---

## 2. Đặc tả Yêu cầu Chức năng (Functional Requirements)

Các yêu cầu được nhóm theo các gói (package) tính năng chính.

### 2.1. Gói Quản lý Dự án (Feature ID: F-PM)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-PM-01** | Tạo Dự án Mới | Hệ thống phải cho phép BA tạo một dự án mới. Hành động này sẽ tạo ra một cấu trúc thư mục chuẩn (ví dụ: `Artifacts/`, `.config/`) tại vị trí do BA chỉ định trên ổ đĩa. | UC-PM-01 |
| **F-PM-02** | Mở Dự án | Hệ thống phải cho phép BA mở một dự án đã tồn tại bằng cách trỏ đến thư mục gốc của dự án. Hệ thống phải xác thực thư mục (ví dụ: tìm file `.config/project.json`) trước khi mở. | UC-PM-02 |
| **F-PM-03** | Import từ Excel | Hệ thống phải cung cấp chức năng "Import from Excel" để cho phép BA "đổ" dữ liệu (ví dụ: Function List, Actor List) từ file `.xlsx` vào dự án. | UC-PM-03 |
| **F-PM-04** | Giao diện Ánh xạ Import | Khi import, hệ thống phải cung cấp một giao diện GUI (Import Wizard) cho phép BA: <br> 1. Ánh xạ (map) một "Sheet" trong Excel với một "Loại Đối tượng" (ví dụ: `Sheet 'Function' -> @FN`). <br> 2. Ánh xạ (map) "Cột" (Column) trong Excel với "Trường" (Field) trong Form (ví dụ: `Cột 'Tên' -> Field 'Name'`). | UC-PM-03 |

### 2.2. Gói Cấu hình Dự án (Feature ID: F-CFG)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-CFG-01** | Trình thiết kế Form (GUI) | (No-Code) Hệ thống phải cung cấp một giao diện GUI "Form Builder" (trong Settings) cho phép BA tạo/sửa/xóa các "Loại Đối tượng" (Artifact Types) (ví dụ: `@UC`, `@BR`). | UC-CFG-01 |
| **F-CFG-02** | Các Loại trường (Fields) | "Form Builder" phải cung cấp một "Toolbox" các loại trường có thể kéo-thả, bao gồm: `Text`, `TextArea`, `Dropdown`, `Linker` (cho `@ID`), và thành phần đặc biệt `Flow Builder`. | UC-CFG-01 |
| **F-CFG-03** | Lưu Cấu hình Form | Khi BA "Save" một thiết kế Form, hệ thống phải lưu cấu hình đó (ví dụ: các trường, thứ tự) dưới dạng một file `.json` (hoặc `.yaml`) trong thư mục `.config`. | UC-CFG-01 |
| **F-CFG-04** | Quản lý Phiên bản (Release) | Hệ thống phải cung cấp một giao diện GUI (trong Settings) cho phép BA tạo/sửa/xóa các "Release" của dự án (ví dụ: `@REL001: V1.0`, `@REL002: V2.0`). | UC-CFG-02 |
| **F-CFG-05** | Trình thiết kế Template Xuất bản | (No-Code) Hệ thống phải cung cấp một giao diện GUI "Export Template Builder" (trong Settings) cho phép BA thiết kế cấu trúc (thứ tự chương, mục) của file PDF/DOCX. | UC-CFG-03 |
| **F-CFG-06** | Chương Động (Dynamic Section) | "Export Template Builder" phải cho phép BA thêm "Chương Động", là một chương được điền dữ liệu tự động bằng "Trình tạo Truy vấn" (Query Builder) trực quan (ví dụ: "Lấy tất cả `@UC` có `Status=Approved`" và "Hiển thị dạng `Table`"). | UC-CFG-03 |
| **F-CFG-07** | Quản lý API Key | Hệ thống phải cung cấp giao diện GUI (trong Settings) để BA nhập và lưu trữ (một cách an toàn) các API Key (ví dụ: Gemini, Figma). | UC-CFG-04 |

### 2.3. Gói Phát triển Yêu cầu (Feature ID: F-DEV)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-DEV-01** | Render Form Động | Khi BA tạo/sửa một đối tượng (ví dụ: `@UC001`), hệ thống phải đọc file template (từ `F-CFG-03`) và **tự động sinh (dynamically render)** ra "Form View" tương ứng với các control (TextField, ComboBox...) | UC-DEV-01 |
| **F-DEV-02** | Nhập liệu Form | "Form View" phải là giao diện **chính và duy nhất** để BA nhập và chỉnh sửa dữ liệu (Source of Truth). | UC-DEV-01 |
| **F-DEV-03** | Giao diện `Flow Builder` | (No-Code) Thành phần `Flow Builder` (trong Form) phải cung cấp một giao diện GUI cho phép BA: <br> 1. Thêm/Xóa/Sắp xếp (kéo-thả) các bước. <br> 2. Chọn Tác nhân (Actor) cho bước. <br> 3. Định nghĩa logic (ví dụ: `If-Then-Else`) qua GUI. | UC-DEV-01 |
| **F-DEV-04** | Tự động lưu (Auto-save) | Hệ thống phải tự động lưu các thay đổi trên Form (vào file `.json`) sau khi BA dừng nhập liệu (ví dụ: 2 giây) mà không cần BA nhấn "Save". | UC-DEV-01 |
| **F-DEV-05** | Ghi đĩa kép (Dual-Write) | Trên mỗi lần lưu (Auto-save), hệ thống phải thực hiện 2 hành động: <br> 1. Ghi file `.json` (Source of Truth). <br> 2. Tự động tạo/cập nhật file `.md` (Git-friendly mirror) từ dữ liệu file `.json` đó. | C-05, C-06 (SRS) |
| **F-DEV-06** | Autocomplete Liên kết | Khi BA gõ `@` trong một trường hỗ trợ, hệ thống phải hiển thị một popup **autocomplete** cho phép tìm kiếm và chèn một liên kết `@ID` (ví dụ: `@BR001`). | UC-DEV-02 |
| **F-DEV-07** | Tương tác Liên kết | Hệ thống phải cung cấp 2 tương tác với tag `@ID` đã chèn: <br> 1. **Hover:** Di chuột qua hiển thị "Hover Card" (tóm tắt thông tin). <br> 2. **Click:** (Ctrl+Click) Mở đối tượng đó trong Tab mới. | UC-DEV-02 |
| **F-DEV-08** | Tích hợp AI (Gemini) | Hệ thống phải cung cấp một nút "Gemini: Đề xuất" (hoặc tương tự) bên cạnh các trường (như `Flow Builder`). Khi nhấn, AI sẽ phân tích ngữ cảnh (ví dụ: Tên UC) và **tự động điền dữ liệu** (ví dụ: các bước flow) vào Form. | UC-DEV-03 |
| **F-DEV-09** | Tích hợp Link Figma | Hệ thống phải cho phép BA dán (paste) một URL Figma vào một trường được chỉ định. Hệ thống phải hiển thị nó dưới dạng hyperlink có thể nhấp (clickable) để mở trong trình duyệt. | UC-DEV-04 |

### 2.4. Gói Mô hình hóa Yêu cầu (Feature ID: F-MOD)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-MOD-01** | Tự động sinh Sơ đồ | Hệ thống phải cung cấp một tab "Diagram View" cho các đối tượng có `Flow Builder`. Tab này phải **tự động sinh** một sơ đồ PlantUML (ví dụ: Sơ đồ Hoạt động) từ dữ liệu đã nhập trong "Form View". | UC-MOD-01 |
| **F-MOD-02** | Sơ đồ Read-only | "Diagram View" phải là **Read-only**. BA không thể chỉnh sửa sơ đồ. Để sửa sơ đồ, BA phải quay lại "Form View" và sửa logic flow. | UC-MOD-01 |
| **F-MOD-03** | Tự động cập nhật Sơ đồ | Khi dữ liệu trong "Form View" được (Auto-save), "Diagram View" phải tự động làm mới (refresh) để phản ánh logic mới. | UC-MOD-01 |
| **F-MOD-04** | Xem Liên kết ngược | Hệ thống phải cung cấp một khu vực (ví dụ: Cột Phải) hiển thị "Backlinks" (Liên kết ngược), là danh sách tất cả các đối tượng khác đang liên kết ĐẾN đối tượng hiện tại. | UC-MOD-03 |
| **F-MOD-05** | Xem Sơ đồ Quan hệ | Hệ thống phải cung cấp một "Project Graph View" (toàn cục) hiển thị tất cả các đối tượng (nodes) và các liên kết `@ID` (edges) giữa chúng. | UC-MOD-02 |
| **F-MOD-06** | Sơ đồ Quy trình Tổng thể | Hệ thống phải hỗ trợ một "Loại Đối tượng" (ví dụ: `@BPMN`) cho phép BA tạo sơ đồ quy trình cấp cao bằng cách liên kết các `@UC` (tasks) lại với nhau. Sơ đồ này phải hỗ trợ "drill-down" (nhấp vào task để mở `@UC`). | UC-MOD-04 |

### 2.5. Gói Quản lý Yêu cầu (Feature ID: F-MGT)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-MGT-01** | Quản lý Tác vụ | Hệ thống phải cho phép BA tạo và quản lý các đối tượng "Task" (`@TSK`) để theo dõi công việc (ví dụ: "Viết draft UC-005"). | UC-MGT-01 |
| **F-MGT-02** | Dashboard Kanban | Hệ thống phải cung cấp một "Kanban View" (trong Dashboard) tự động đọc và hiển thị tất cả các đối tượng (`@UC`, `@BR`...) vào các cột dựa trên trường "Status" (Trạng thái) của chúng. | UC-MGT-02 |
| **F-MGT-03** | Cập nhật Kanban | BA phải có thể kéo-thả các thẻ (cards) trong "Kanban View" từ cột này sang cột khác. Hành động này phải **tự động cập nhật** trường "Status" của đối tượng và lưu (Auto-save). | UC-MGT-02 |
| **F-MGT-04** | Dashboard Gantt | Hệ thống phải cung cấp một "Gantt View" (trong Dashboard) tự động đọc tất cả các đối tượng `@TSK` và hiển thị chúng trên một biểu đồ timeline dựa trên trường "Due Date". | UC-MGT-02 |
| **F-MGT-05** | Gán Phiên bản (Release) | BA phải có khả năng gán một Đối tượng Yêu cầu (ví dụ: `@UC001`) vào một "Release" (ví dụ: `@REL001`) thông qua một trường "Target Release" trong Form. | UC-MGT-03 |

### 2.6. Gói Xuất bản Tài liệu (Feature ID: F-PUB)

| ID | Yêu cầu | Mô tả chi tiết | Use Case |
| :--- | :--- | :--- | :--- |
| **F-PUB-01** | Xuất ra PDF/DOCX | Hệ thống phải cung cấp chức năng "Export to Document (PDF/DOCX)". | UC-PUB-01 |
| **F-PUB-02** | Lọc theo Phiên bản | Chức năng Export (F-PUB-01) phải cho phép BA **lọc** (filter) các yêu cầu sẽ được xuất bản dựa trên "Release" đã được gán (từ `F-MGT-05`). | UC-PUB-01 |
| **F-PUB-03** | Tuân thủ Template | Chức năng Export (F-PUB-01) phải gom (aggregate) nội dung và định dạng file đầu ra theo đúng cấu trúc (thứ tự chương, mục) đã được BA thiết kế trong "Export Template" (từ `F-CFG-05`). | UC-PUB-01 |
| **F-PUB-04** | Xuất ra Excel | Hệ thống phải cung cấp chức năng "Export to Excel", tạo ra một file `.xlsx` trong đó mỗi "Loại Đối tượng" (`@UC`, `@BR`...) là một "Sheet" riêng biệt. | UC-PUB-02 |

---

## 3. Yêu cầu Giao diện (User Interface Requirements)

| ID | Yêu cầu | Mô tả chi tiết |
| :--- | :--- | :--- |
| **UI-01** | Bố cục IntelliJ | Giao diện phải tuân theo bố cục 3 cột: Cột Trái (Project TreeView), Cột Giữa (Main Work Area - TabPane), Cột Phải (Contextual Info - Backlinks, Details). |
| **UI-02** | Đa cửa sổ | Hệ thống phải hỗ trợ BA kéo một `Tab` từ `TabPane` chính ra ngoài để trở thành một cửa sổ (Stage) riêng biệt. |
| **UI-03** | Theme (Chủ đề) | Hệ thống (v1.0) phải cung cấp một (và chỉ một) chủ đề "Dark Mode" (tối màu). |
| **UI-04** | Tùy chỉnh CSS | File CSS định nghĩa theme (UI-03) phải được lưu dưới dạng file bên ngoài (external file), cho phép người dùng nâng cao tự tùy chỉnh. |
| **UI-05** | Nguyên tắc "No-Code" | Tất cả các chức năng cấu hình (F-CFG) phải được thực hiện 100% qua giao diện GUI (kéo-thả, điền form). BA không bao giờ bị bắt buộc phải sửa file `.json` hay viết code. |

---

## 4. Yêu cầu Phi chức năng (Non-Functional Requirements)

| ID | Yêu cầu | Mô tả chi tiết |
| :--- | :--- | :--- |
| **NFR-01** | Lưu trữ (Local-first) | Hệ thống phải lưu trữ tất cả dữ liệu dự án trên hệ thống file local của người dùng. Không được phụ thuộc vào cơ sở dữ liệu (SQL, NoSQL) bên ngoài. |
| **NFR-02** | Tương thích Git | Hệ thống phải tuân thủ kiến trúc "Dual-Write" (`.json` làm Source of Truth, `.md` làm Git-friendly mirror) để đảm bảo mọi thay đổi đều có thể được "diff" và theo dõi bởi Git. |
| **NFR-03** | Tự động lưu (Reliability) | Hệ thống phải có cơ chế auto-save (kích hoạt sau 1-2 giây idle) để ngăn ngừa mất dữ liệu. |
| **NFR-04** | Hiệu suất (Performance) | <br> - Render Form (F-DEV-01): < 500ms. <br> - Render Sơ đồ (F-MOD-01): < 1 giây (cho luồng < 50 bước). <br> - Autocomplete (F-DEV-06): < 300ms. |
| **NFR-05** | Công nghệ (Constraints) | Phải được xây dựng bằng Java 17+ và JavaFX 17+. |
| **NFR-06** | Kiến trúc (Constraints) | Phải tuân thủ MVVM, SOLID, và sử dụng Dependency Injection (Guice). |
| **NFR-07** | Phụ thuộc (Constraints) | Phải sử dụng thư viện **PlantUML** (Java) cho (F-MOD-01) và **Pandoc** (CLI) cho (F-PUB-01). |

---

## 5. Tham chiếu

* `Software Requirement Specification.md`
* `Architecture Description.md`
* Thư mục `UseCases/` (chứa tất cả các file UC chi tiết)