# Kế hoạch Thực thi Dự án (Implementation Plan): RMS v1.0

## 1. Tổng quan

* **Mục tiêu:** Xây dựng một ứng dụng RMS (v1.0) với các tính năng cốt lõi đã định nghĩa.
* **Phương pháp:** Agile (Iterative Development), chia nhỏ thành các "Lát cắt Dọc" (Vertical Slices).
* **Ước tính tổng thể:** 34 ngày công (Tham vọng).
* **Định nghĩa "Hoàn thành" (Definition of Done) mỗi ngày:** Chức năng có thể đóng gói (build), kiểm thử (testable), và demo được (demoable).

---

## 2. Giai đoạn 0: Nền tảng (Foundation & Setup)

* **Mục tiêu:** Tạo "bộ khung" của ứng dụng, đảm bảo kiến trúc MVVM, DI và UI cơ bản hoạt động.
* **Thời lượng:** 3 ngày.

| Ngày | Công việc Cốt lõi | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 1 | Khởi tạo Dự án | - Thiết lập Maven/Gradle (JavaFX, Jackson, Guice, PlantUML).<br>- Khởi tạo Git.<br>- Tạo cấu trúc package MVVM, Service, Repo.<br>- Cấu hình Google Guice (DI). | Dự án build thành công. Ứng dụng chạy, hiển thị một cửa sổ JavaFX trống. |
| 2 | Dựng Khung UI (Shell) | - Tạo `MainView.fxml` (layout 3 cột: `BorderPane`).<br>- Thêm `TreeView` (Trái), `TabPane` (Giữa), `Accordion` (Phải) (dùng dummy data). | Chạy ứng dụng. Giao diện 3 cột hiển thị đúng (IntelliJ-like). |
| 3 | Hoàn thiện Vỏ (Shell) | - Áp dụng file `dark-theme.css` cho toàn bộ ứng dụng.<br>- Triển khai `StageManager` (Service) để quản lý Đa cửa sổ (Multi-window). | Giao diện có màu tối. Có thể kéo một `Tab` ra thành một cửa sổ (Stage) mới. |

---

## 3. Giai đoạn 1: Lõi Quản lý Dự án (Project Core)

* **Mục tiêu:** Triển khai các chức năng cơ bản nhất: tạo, mở dự án và truy cập file.
* **Thời lượng:** 3 ngày.

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 4 | Tạo Dự án (UC-PM-01) | - Triển khai `ProjectService.createProject()`.<br>- UI Dialog (chọn tên, vị trí).<br>- Logic tạo cấu trúc thư mục (`.config`, `Artifacts`). | Chạy app -> File > New Project. Kiểm tra thư mục được tạo ra đúng cấu trúc. |
| 5 | Mở Dự án (UC-PM-02) | - Triển khai `ProjectService.openProject()`.<br>- UI Dialog (chọn thư mục).<br>- Logic đọc cấu trúc thư mục và hiển thị lên `TreeView` (cột trái). | Chạy app -> File > Open Project. `TreeView` hiển thị cấu trúc thư mục. |
| 6 | Lõi Repository | - Định nghĩa `IArtifactRepository` (Interface).<br>- Triển khai `JsonFileRepository` (dùng Jackson).<br>- Logic `save(obj)` và `load(id)`. | Viết **JUnit Test** cho `JsonFileRepository`. Test (lưu/tải) một POJO đơn giản thành công. |

---

## 4. Giai đoạn 2: Lõi Cấu hình (Configuration Core)

* **Mục tiêu:** Triển khai "trái tim" của ứng dụng: `UC-CFG-01` (Form Builder) và `FormRenderer`.
* **Thời lượng:** 5 ngày.

| Ngày | Công việc Cốt lõi | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 7 | UI Form Builder (UC-CFG-01) | - Thiết kế `FormBuilderView.fxml` (Settings > Artifact Types).<br>- Bố cục 3 cột (Toolbox, Preview, Properties). | Mở màn hình Settings. Giao diện Form Builder hiển thị (chưa có logic). |
| 8 | Logic Kéo-thả (Form Builder) | - Triển khai logic Drag-and-Drop (kéo trường từ `Toolbox` vào `Preview`).<br>- Triển khai `FormBuilderViewModel`. | Kéo "Text Field" vào "Preview", nó hiển thị. Kéo-thả sắp xếp các trường. |
| 9 | Logic Lưu Template (Form Builder) | - Triển khai `TemplateService.saveTemplate()`.<br>- Serialize thiết kế Form (từ ViewModel) thành file `.json`. | Thiết kế 1 form (2 trường) -> "Save". Kiểm tra file `[TemplateName].json` sinh ra đúng. |
| 10 | Logic Render Form (Phần 1) | - Triển khai `FormRenderer.render(template)` (Service).<br>- Logic đọc `.json` (từ Ngày 9), tự động sinh control `TextField`, `TextArea`. | **(Lát cắt dọc)** 1. Dùng Form Builder (Ngày 9) tạo template "Test". 2. Nhấn "New > Test" -> App hiển thị Tab mới với Form đã được render tự động. |
| 11 | Logic Render Form (Phần 2) | - Mở rộng `FormRenderer` để render 2 loại phức tạp: `Dropdown` và `Linker` (dùng cho `@ID`). | Lặp lại test Ngày 10 với template mới. Form mới hiển thị `ComboBox` và control `Linker`. |

---

## 5. Giai đoạn 3: Lõi Phát triển Yêu cầu (Development Core)

* **Mục tiêu:** Triển khai các chức năng nhập liệu chính (Editor) cho BA.
* **Thời lượng:** 6 ngày.

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 12 | Logic Auto-Save (UC-DEV-01) | - Triển khai `ArtifactViewModel` (ViewModel).<br>- Dùng `PauseTransition` (JavaFX) để kích hoạt `IArtifactRepository.save()` 2s sau khi dừng gõ. | Mở Form (từ Ngày 11) -> Gõ chữ -> Dừng 2s. Kiểm tra file `[ID].json` được tạo/cập nhật. |
| 13 | Logic Dual-Write (Git-Friendly) | - Mở rộng `JsonFileRepository.save()` (Repository).<br>- Thêm logic: Sau khi lưu `.json`, **tự động sinh** file `.md` (mirror) tương ứng. | Lặp lại test Ngày 12. Kiểm tra file `[ID].md` cũng được tạo/cập nhật. |
| 14 | UI Flow Builder (UC-DEV-01) | - Thiết kế UI (FXML) cho `Flow Builder` (dạng `TableView` hoặc `GridPane` động).<br>- Tích hợp `Flow Builder` vào `FormRenderer` (như một loại trường). | 1. Thêm `Flow Builder` vào template "Use Case" (Ngày 9). 2. Mở "New > Use Case" -> Thấy giao diện `Flow Builder` (thêm/xóa hàng, chọn Actor...). |
| 15 | Logic Flow Builder (Form) | - Hoàn thiện logic `Flow Builder` (thêm/xóa bước `If/Then`, kéo-thả sắp xếp).<br>- Đảm bảo `ArtifactViewModel` cập nhật đúng cấu trúc `Flow` trong Model. | Thêm 5 bước, kéo-thả bước 5 lên bước 2. Thêm "If". Save. Kiểm tra file `.json` lưu đúng cấu trúc lồng nhau (nested). |
| 16 | Logic Liên kết (UC-DEV-02) | - Triển khai `SearchService` (quét và cache tất cả `@ID`).<br>- Triển khai logic **Autocomplete** (popup `@ID`). | 1. Tạo `@BR001`. 2. Mở `@UC001`. 3. Gõ `@B` vào `TextArea` -> Popup PHẢI hiển thị `@BR001`. |
| 17 | Logic Liên kết (Navigate) | - Triển khai "Hover Card" (hiển thị tóm tắt khi di chuột qua `@ID`).<br>- Triển khai "Click-to-Navigate" (Ctrl+Click `@ID` mở Tab mới). | Di chuột qua tag `@BR001` -> Tooltip hiện ra. Ctrl+Click -> Tab `@BR001` được mở. |

---

## 6. Giai đoạn 4: Lõi Mô hình hóa (Modeling Core)

* **Mục tiêu:** Tự động sinh sơ đồ, tính năng "ăn tiền" của app.
* **Thời lượng:** 4 ngày.

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 18 | Tích hợp PlantUML | - Thêm thư viện PlantUML (`.jar`) vào Maven/Gradle.<br>- Tạo `DiagramRenderService` (phần 1). Hàm `render(string)` trả về `BufferedImage`. | Viết **JUnit Test**. Gọi `render("@startuml\n a->b \n@enduml")` -> Kiểm tra `BufferedImage` trả về (không null). |
| 19 | Sinh Code Sơ đồ (UC-MOD-01) | - Hoàn thiện `DiagramRenderService` (phần 2).<br>- Logic đọc `Flow` (từ ViewModel / `.json` ở Ngày 15) và **tự động sinh (generate) ra chuỗi code PlantUML**. | Viết **JUnit Test**. Tạo 1 `Flow` (3 bước, 1 If) -> Gọi hàm -> Kiểm tra chuỗi code PlantUML sinh ra là chính xác. |
| 20 | Hiển thị Sơ đồ (UC-MOD-01) | - Tạo Tab "Diagram View" (FXML).<br>- `ArtifactViewModel` gọi `DiagramRenderService` -> Nhận `BufferedImage` -> Hiển thị trong `ImageView`. | Mở `@UC001` (đã có flow) -> Chuyển sang Tab "Diagram View" -> Sơ đồ PlantUML PHẢI hiển thị chính xác. |
| 21 | Backlinks (UC-MOD-03) | - Mở rộng `SearchService` (Ngày 16) để xây dựng chỉ mục "Backlinks".<br>- Hiển thị danh sách Backlinks lên Cột Phải (UI đã tạo ở Ngày 2). | 1. Mở `@UC001` (đã link tới `@BR001`). 2. Mở `@BR001` -> Cột Phải PHẢI hiển thị "Backlinks: @UC001". |

---

## 7. Giai đoạn 5: Lõi Quản lý (Management Core)

* **Mục tiêu:** Thêm lớp quản lý tiến độ và phiên bản.
* **Thời lượng:** 4 ngày.

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 22 | Quản lý Release (UC-CFG-02, UC-MGT-03) | - Tạo UI "Settings > Releases" (để tạo `@REL001: V1.0`).<br>- Thêm trường "Target Release" (dạng `Linker`) vào `Form Builder`. | 1. Tạo `@REL001`. 2. Mở `@UC001`, gán nó cho `@REL001` -> Save. 3. Kiểm tra file `.json` đã lưu liên kết. |
| 23 | Quản lý Task (UC-MGT-01) | - Dùng `Form Builder` (Ngày 9) để tạo template "Task" (`@TSK`) với trường "Status" (Todo/Done) và "Due Date". | Tạo mới một `@TSK001` -> Điền thông tin -> Save. Kiểm tra file `.json` được lưu. |
| 24 | Kanban View (UC-MGT-02) | - Tạo UI "Dashboard View" (Tab mới) với chế độ "Kanban".<br>- Logic: Quét tất cả artifact, đọc trường "Status", và hiển thị (dạng Thẻ - Card) vào các Cột. | Mở "Dashboard" -> Thấy thẻ `@UC001` và `@TSK001` nằm đúng cột "Status". |
| 25 | Kanban (Kéo-thả) & Gantt View | - Triển khai logic kéo-thả cho "Kanban View" (kéo thẻ -> tự động cập nhật "Status" và Save).<br>- (Nếu còn thời gian) Triển khai "Gantt View". | Kéo `@UC001` từ "Draft" sang "Approved" -> Mở lại `@UC001` -> "Status" PHẢI là "Approved". |

---

## 8. Giai đoạn 6: Lõi Xuất bản (Publication Core)

* **Mục tiêu:** Hoàn thiện vòng đời: lấy dữ liệu ra khỏi hệ thống.
* **Thời lượng:** 5 ngày.

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 26 | Xuất Excel (UC-PUB-02) | - Tích hợp **Apache POI**.<br>- Triển khai `ExportService.exportToExcel()`. Logic: Quét artifact -> Tạo `Sheet` cho mỗi Loại -> Ghi dữ liệu vào hàng/cột. | File > Export > Excel. Kiểm tra file `.xlsx` được tạo ra. |
| 27 | Tích hợp Pandoc (UC-PUB-01) | - Cài đặt Pandoc (thủ công).<br>- Triển khai `ExportService.exportToPdf(stringMd)`. Logic: Lưu `temp.md` -> Gọi `ProcessBuilder` để chạy lệnh `pandoc`. | Viết **JUnit Test**. Gọi `exportToPdf("## Test")` -> Kiểm tra file `test.pdf` được tạo ra. |
| 28 | UI Template Xuất bản (UC-CFG-03) | - Thiết kế UI "Settings > Export Templates" (cho phép Thêm/Xóa/Sắp xếp "Chương").<br>- UI cho "Chương Động" (chọn `@UC`, `@BR`, chọn kiểu `Table`/`Full`). | Mở Settings -> Thấy UI. |
| 29 | Logic Template Xuất bản (UC-CFG-03) | - Triển khai logic lưu/tải Template Xuất bản (serialize thiết kế UI thành file `srs_template.json`). | Thiết kế 1 template (1 Tĩnh, 1 Động) -> Save. Kiểm tra file `.json` được tạo đúng. |
| 30 | Logic Xuất bản (UC-PUB-01) | - Triển khai logic `ExportService.exportToDocument()`.<br>- Logic: Đọc template `.json` (Ngày 29) -> Lọc artifact theo Release (Ngày 22) -> Gom file `.md` -> Gọi Pandoc (Ngày 27). | File > Export > PDF -> Chọn template và Release. Kiểm tra file `My_SRS_V1.pdf` được tạo ra đúng cấu trúc. |

---

## 9. Giai đoạn 7: Tích hợp Nâng cao & Hoàn thiện

* **Mục tiêu:** Thêm các tính năng "wow" và đóng gói sản phẩm.
* **Thời lượng:** 4 ngày.

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 31 | Import Excel (UC-PM-03) | - Triển khai logic ngược của Ngày 26.<br>- Tạo UI "Import Wizard" cho phép ánh xạ (map) cột Excel với trường Form. | Lấy file Excel (Ngày 26) -> Import -> Kiểm tra các artifact mới được tạo ra trong `TreeView`. |
| 32 | Tích hợp Gemini (UC-DEV-03) | - Tạo `ApiService` (dùng `HttpClient`).<br>- Thêm nút "Gemini" vào `Flow Builder`. Logic: Gửi prompt -> Nhận JSON (các bước) -> Tự động điền vào Form. | Mở `@UC001` -> Nhấn "Gemini" -> `Flow Builder` được điền tự động. |
| 33 | Sơ đồ Quan hệ (UC-MOD-02) | - Tích hợp thư viện JavaFX Graph (ví dụ: GraphFX hoặc `WebView` + `vis.js`).<br>- Tạo UI "Project Graph". Lấy dữ liệu (Nodes/Edges) từ `SearchService` (Ngày 16) và render. | Mở "Graph View" -> Thấy node `@UC001` và `@BR001` được nối với nhau. |
| 34 | Hoàn thiện & Đóng gói | - Tích hợp Figma (Link đơn giản `UC-DEV-04`).<br>- Rà soát lỗi (Bug fixing).<br>- Sử dụng `jpackage` để đóng gói ứng dụng thành file cài đặt (`.exe`, `.dmg`). | File cài đặt `RMS-1.0.exe` (hoặc `.dmg`). Cài đặt và chạy ứng dụng thành công. |