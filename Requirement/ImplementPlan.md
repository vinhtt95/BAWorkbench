# Kế hoạch Thực thi Dự án (Implementation Plan): RMS v1.0
# (Cập nhật v2: Chèn SQLite sau Giai đoạn 3)

## 1. Tổng quan

* **Mục tiêu:** Xây dựng một ứng dụng RMS (v1.0) với các tính năng cốt lõi đã định nghĩa.
* **Phương pháp:** Agile (Iterative Development), chia nhỏ thành "Lát cắt Dọc".
* **Ước tính tổng thể:** 38 ngày công.
* **Hiện trạng (Theo yêu cầu):** Đã hoàn thành Giai đoạn 0, 1, 2. Đang thực hiện Giai đoạn 3.

---

## 2. Giai đoạn 0: Nền tảng (Foundation & Setup)

* **Thời lượng:** 3 ngày (Đã hoàn thành)

| Ngày | Công việc Cốt lõi | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 1 | Khởi tạo Dự án | - Thiết lập Maven/Gradle (JavaFX, Jackson, Guice, PlantUML).<br>- Khởi tạo Git.<br>- Cấu trúc package MVVM, Service, Repo. | Dự án build thành công. Ứng dụng chạy, cửa sổ trống. |
| 2 | Dựng Khung UI (Shell) | - Tạo `MainView.fxml` (layout 3 cột: `BorderPane`).<br>- Thêm `TreeView`, `TabPane`, `Accordion` (dummy data). | Chạy ứng dụng. Giao diện 3 cột hiển thị đúng. |
| 3 | Hoàn thiện Vỏ (Shell) | - Áp dụng file `dark-theme.css`.<br>- Triển khai `StageManager` (Service) để quản lý Đa cửa sổ. | Giao diện có màu tối. Có thể kéo một `Tab` ra cửa sổ mới. |

---

## 3. Giai đoạn 1: Lõi Quản lý Dự án (Project Core)

* **Thời lượng:** 3 ngày (Đã hoàn thành)

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 4 | Tạo Dự án (UC-PM-01) | - Triển khai `ProjectService.createProject()`.<br>- UI Dialog (chọn tên, vị trí).<br>- Logic tạo cấu trúc thư mục (`.config`, `Artifacts`). | Chạy app -> New Project. Kiểm tra thư mục được tạo đúng cấu trúc. |
| 5 | Mở Dự án (UC-PM-02) | - Triển khai `ProjectService.openProject()` (chưa có index).<br>- UI Dialog (chọn thư mục).<br>- Logic đọc cấu trúc thư mục và hiển thị lên `TreeView`. | Chạy app -> Open Project. `TreeView` hiển thị cấu trúc thư mục. |
| 6 | Lõi Repository (Files) | - Định nghĩa `IArtifactRepository`.<br>- Triển khai `JsonFileRepository` (chỉ ghi `.json`, `.md`).<br>- Logic `save(obj)` và `load(id)`. | **JUnit Test** cho `JsonFileRepository`. Test (lưu/tải) POJO thành công. |

---

## 4. Giai đoạn 2: Lõi Cấu hình (Configuration Core)

* **Thời lượng:** 5 ngày (Đã hoàn thành)

| Ngày | Công việc Cốt lõi | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 7 | UI Form Builder (UC-CFG-01) | - Thiết kế `FormBuilderView.fxml` (Settings > Artifact Types).<br>- Bố cục 3 cột (Toolbox, Preview, Properties). | Mở màn hình Settings. Giao diện Form Builder hiển thị. |
| 8 | Logic Kéo-thả (Form Builder) | - Triển khai logic Drag-and-Drop (kéo trường từ `Toolbox` vào `Preview`).<br>- Triển khai `FormBuilderViewModel`. | Kéo "Text Field" vào "Preview", nó hiển thị. Kéo-thả sắp xếp. |
| 9 | Logic Lưu Template (Form Builder) | - Triển khai `TemplateService.saveTemplate()`.<br>- Serialize thiết kế Form thành file `.template.json`. | Thiết kế 1 form -> "Save". Kiểm tra file `.json` sinh ra đúng. |
| 10 | Logic Render Form (Phần 1) | - Triển khai `FormRenderer.render(template)`.<br>- Logic đọc `.json`, tự động sinh control `TextField`, `TextArea`. | **(Lát cắt dọc)** 1. Dùng Form Builder (Ngày 9) tạo template "Test". 2. Nhấn "New > Test" -> App hiển thị Tab mới với Form đã render. |
| 11 | Logic Render Form (Phần 2) | - Mở rộng `FormRenderer` để render `Dropdown` và `Linker` (dùng cho `@ID`). | Lặp lại test Ngày 10. Form mới hiển thị `ComboBox` và control `Linker`. |

---

## 5. Giai đoạn 3: Lõi Phát triển Yêu cầu (Development Core)

* **Thời lượng:** 6 ngày (Đang thực hiện)
* **Lưu ý:** Các tính năng (Autocomplete, Backlinks) ở giai đoạn này sẽ **chạy chậm** (vì quét file). Chúng sẽ được *tái cấu trúc (refactor)* ở Giai đoạn 5.

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 12 | Logic Auto-Save (UC-DEV-01) | - Triển khai `ArtifactViewModel`.<br>- Dùng `PauseTransition` kích hoạt `IArtifactRepository.save()` (Ghi 2 file `.json`, `.md`). | Mở Form (Ngày 11) -> Gõ chữ -> Dừng 2s. Kiểm tra file `.json` và `.md` được tạo/cập nhật. |
| 13 | UI Flow Builder (UC-DEV-01) | - Thiết kế UI (FXML) cho `Flow Builder` (dạng `TableView` hoặc `GridPane` động).<br>- Tích hợp `Flow Builder` vào `FormRenderer`. | 1. Thêm `Flow Builder` vào template "Use Case". 2. Mở "New > Use Case" -> Thấy giao diện `Flow Builder` (thêm/xóa hàng, chọn Actor...). |
| 14 | Logic Flow Builder (Form) | - Hoàn thiện logic `Flow Builder` (thêm/xóa bước `If/Then`, kéo-thả sắp xếp).<br>- Đảm bảo `ArtifactViewModel` cập nhật đúng cấu trúc `Flow` trong Model. | Thêm 5 bước, kéo-thả bước 5 lên bước 2. Thêm "If". Save. Kiểm tra file `.json` lưu đúng cấu trúc lồng nhau (nested). |
| 15 | Logic Liên kết (UC-DEV-02) | - Triển khai `SearchService` (phiên bản 1, **quét file system**).<br>- Triển khai logic **Autocomplete** (popup `@ID`). | 1. Tạo `@BR001`. 2. Mở `@UC001`. 3. Gõ `@B` -> Popup PHẢI hiển thị `@BR001` (có thể hơi chậm). |
| 16 | Logic Liên kết (Navigate) | - Triển khai "Hover Card" (đọc file).<br>- Triển khai "Click-to-Navigate" (mở Tab mới). | Di chuột qua tag `@BR001` -> Tooltip hiện ra. Ctrl+Click -> Tab `@BR001` được mở. |
| 17 | Backlinks (UC-MOD-03) | - Logic `SearchService.getBacklinks(id)` (phiên bản 1, **quét toàn bộ file**).<br>- Hiển thị danh sách Backlinks lên Cột Phải. | 1. Mở `@UC001` (đã link tới `@BR001`). 2. Mở `@BR001` -> Cột Phải PHẢI hiển thị "Backlinks: @UC001" (có thể hơi chậm). |

---

## 6. Giai đoạn 4: Lõi Chỉ mục (Indexing Core) (MỚI)

* **Mục tiêu:** Tích hợp SQLite để giải quyết vấn đề hiệu năng và toàn vẹn, chuẩn bị cho các giai đoạn sau.
* **Thời lượng:** 3 ngày (Bắt đầu từ Ngày 18)

| Ngày | Công việc Cốt lõi | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 18 | Tích hợp SQLite | - Thêm dependency `sqlite-jdbc` vào Maven/Gradle.<br>- Tạo `SqliteIndexRepository` (Repo) với các hàm: `initDB()`, `clearIndex()`, `insertArtifact()`, `insertLink()`, `queryArtifacts()`, `queryLinks()`. | **JUnit Test** cho `SqliteIndexRepository`. Test (khởi tạo, ghi, đọc) DB thành công. |
| 19 | Logic Lập Chỉ mục (UC-PM-04) | - Triển khai `IndexService.validateAndRebuildIndex()`.<br>- Logic: Quét thư mục `Artifacts/` (quét `.json`), và ghi vào `index.db` (dùng `SqliteIndexRepository`).<br>- Cập nhật `UC-PM-02` (Mở Dự án) để tự động gọi `RebuildIndex()` (trên luồng nền).<br>- Cập nhật `.gitignore` (trong `UC-PM-01`) để bỏ qua `index.db`. | 1. Mở dự án (Ngày 5) -> `index.db` được tạo. 2. Thêm file `.json` thủ công -> Mở lại dự án -> `index.db` được cập nhật. |
| 20 | Cập nhật Repository (Toàn vẹn) | - Cập nhật `IArtifactRepository` (thêm `checkLinks`).<br>- Cập nhật `JsonFileRepository.save()` -> Phải gọi `IndexService.updateArtifactInIndex()` (Triple-write).<br>- Cập nhật `JsonFileRepository.delete()` -> Phải gọi `IndexService.checkBacklinks()` (và ném lỗi) và `IndexService.deleteArtifactFromIndex()`. | 1. Tạo artifact (Ngày 15) -> `index.db` được cập nhật. 2. Xóa artifact có link (Ngày 17) -> App PHẢI báo lỗi và ngăn chặn. |

---

## 7. Giai đoạn 5: Tái cấu trúc (Refactoring) (MỚI)

* **Mục tiêu:** Cập nhật các tính năng *đã làm* (GĐ 3) để chúng sử dụng Lớp Chỉ mục SQLite mới.
* **Thời lượng:** 3 ngày (Bắt đầu từ Ngày 21)

| Ngày | Công việc Cốt lõi | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 21 | Refactor Autocomplete (GĐ 3) | - Cập nhật `SearchService.autocomplete()` (Ngày 15).<br>- Logic mới: **PHẢI truy vấn `index.db` (SQLite)** (thay vì quét file system). | Lặp lại test Ngày 15. Popup `@ID` PHẢI hiển thị **ngay lập tức** (< 300ms). |
| 22 | Refactor Backlinks (GĐ 3) | - Cập nhật `SearchService.getBacklinks()` (Ngày 17).<br>- Logic mới: **PHẢI truy vấn `index.db` (SQLite)**. | Lặp lại test Ngày 17. Mở `@BR001` -> Cột Phải PHẢI hiển thị Backlinks **ngay lập tức**. |
| 23 | Refactor Xử lý Lỗi (GĐ 3) | - Cập nhật `ArtifactViewModel` (hoặc nơi gọi `delete()`).<br>- Phải `try-catch` lỗi `IntegrityViolationException` (từ Ngày 20) và hiển thị Alert (JavaFX) thân thiện cho BA (ví dụ: "Không thể xóa..."). | 1. Mở `@BR001` (đang được link). 2. Nhấn "Delete". 3. Ứng dụng PHẢI hiển thị Alert cảnh báo (thay vì crash). |

---

## 8. Giai đoạn 6: Lõi Mô hình hóa (Modeling Core)

* **Thời lượng:** 3 ngày (Bắt đầu từ Ngày 24)

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 24 | Tích hợp PlantUML | - Thêm thư viện PlantUML (`.jar`).<br>- Tạo `DiagramRenderService` (phần 1). Hàm `render(string)` trả về `BufferedImage`. | **JUnit Test** cho `render("@startuml\n a->b \n@enduml")`. |
| 25 | Sinh Code Sơ đồ (UC-MOD-01) | - Hoàn thiện `DiagramRenderService` (phần 2).<br>- Logic đọc `Flow` (từ ViewModel) và **sinh ra chuỗi code PlantUML**. | **JUnit Test** cho `Flow` (3 bước, 1 If) -> Kiểm tra chuỗi code PlantUML. |
| 26 | Hiển thị Sơ đồ (UC-MOD-01) | - Tạo Tab "Diagram View" (FXML).<br>- Tích hợp (Ngày 24 + 25): `ArtifactViewModel` gọi `RenderService` -> Hiển thị `BufferedImage` trong `ImageView`. | Mở `@UC001` (có flow) -> Chuyển Tab "Diagram View" -> Sơ đồ PlantUML hiển thị chính xác. |

---

## 9. Giai đoạn 7: Lõi Quản lý (Management Core)

* **Thời lượng:** 3 ngày (Bắt đầu từ Ngày 27)

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 27 | Quản lý Release & Task (UC-CFG-02, UC-MGT-01, UC-MGT-03) | - Tạo UI "Settings > Releases" (tạo `@REL001`).<br>- Dùng Form Builder tạo template "Task" (`@TSK`).<br>- Thêm trường "Target Release" vào template `@UC`. | 1. Tạo `@REL001`. 2. Tạo `@TSK001`. 3. Gán `@UC001` cho `@REL001`. |
| 28 | Kanban View (UC-MGT-02) | - Tạo UI "Dashboard View" (Tab mới) với chế độ "Kanban".<br>- Logic: **Truy vấn `index.db` (SQLite)** (theo "Status") và hiển thị các Thẻ (Card). | Mở "Dashboard" -> Thấy `@UC001` và `@TSK001` nằm đúng cột (đọc từ DB). |
| 29 | Kanban (Kéo-thả) & Gantt View | - Triển khai logic kéo-thả cho "Kanban View" (kéo thẻ -> cập nhật "Status" -> kích hoạt Auto-save).<br>- Triển khai "Gantt View" (đọc `@TSK` từ `index.db`). | Kéo `@UC001` từ "Draft" sang "Approved" -> Mở lại `@UC001` -> "Status" PHẢI là "Approved". |

---

## 10. Giai đoạn 8: Lõi Xuất bản (Publication Core)

* **Thời lượng:** 4 ngày (Bắt đầu từ Ngày 30)

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 30 | Xuất Excel (UC-PUB-02) | - Tích hợp **Apache POI**.<br>- Triển khai `ExportService.exportToExcel()`. Logic: **Truy vấn `index.db` (SQLite)** (thay vì quét file) -> Ghi dữ liệu vào `Sheet`. | File > Export > Excel. Kiểm tra file `.xlsx` được tạo ra. |
| 31 | Tích hợp Pandoc (UC-PUB-01) | - Cài đặt Pandoc (thủ công).<br>- Triển khai `ExportService.exportToPdf(stringMd)`. Logic: Lưu `temp.md` -> Gọi `ProcessBuilder` chạy lệnh `pandoc`. | **JUnit Test**. Gọi `exportToPdf("## Test")` -> Kiểm tra file `test.pdf` được tạo ra. |
| 32 | UI & Logic Template Xuất bản (UC-CFG-03) | - Thiết kế UI "Settings > Export Templates" (Thêm/Xóa/Sắp xếp "Chương").<br>- Triển khai logic lưu/tải Template (serialize thành `srs_template.json`). | Thiết kế 1 template (1 Tĩnh, 1 Động) -> Save. Kiểm tra file `.json` được tạo đúng. |
| 33 | Logic Xuất bản (UC-PUB-01) | - Triển khai logic `ExportService.exportToDocument()`.<br>- Logic: Đọc template `.json` -> Lọc artifact (truy vấn `index.db` theo Release) -> Gom file `.md` -> Gọi Pandoc (Ngày 31). | File > Export > PDF -> Chọn template và Release. Kiểm tra file `My_SRS_V1.pdf` được tạo ra đúng cấu trúc. |

---

## 11. Giai đoạn 9: Tích hợp Nâng cao & Hoàn thiện

* **Thời lượng:** 5 ngày (Bắt đầu từ Ngày 34)

| Ngày | Công việc Cốt lõi (UC) | Chi tiết | Đầu ra (Testable) |
| :--- | :--- | :--- | :--- |
| 34 | Import Excel (UC-PM-03) | - Triển khai logic ngược của Ngày 30.<br>- Tạo UI "Import Wizard" (ánh xạ cột).<br>- **Quan trọng:** Sau khi import (tạo file `.json`), phải gọi `IndexService.rebuildIndex()`. | Lấy file Excel (Ngày 30) -> Import -> `TreeView` cập nhật VÀ `index.db` cập nhật. |
| 35 | Tích hợp Gemini (UC-DEV-03) | - Tạo `ApiService` (dùng `HttpClient`).<br>- Thêm nút "Gemini" vào `Flow Builder`. Logic: Gửi prompt -> Nhận JSON (các bước) -> Tự động điền vào Form. | Mở `@UC001` -> Nhấn "Gemini" -> `Flow Builder` được điền tự động. |
| 36 | Sơ đồ Quan hệ (UC-MOD-02) | - Tích hợp thư viện JavaFX Graph (hoặc `WebView` + `vis.js`).<br>- Tạo UI "Project Graph". Lấy dữ liệu (Nodes/Edges) từ `index.db` (qua `SearchService`) và render. | Mở "Graph View" -> Thấy node `@UC001` và `@BR001` được nối với nhau. |
| 37 | Hoàn thiện (Figma, Re-index) | - Tích hợp Figma (Link đơn giản `UC-DEV-04`).<br>- Thêm nút "Kiểm tra & Tái lập Chỉ mục" (<code>UC-PM-04</code>) vào Menu UI một cách rõ ràng. | Dán link Figma vào Form -> Thấy link. Chạy `UC-PM-04` thủ công -> Ứng dụng hoạt động. |
| 38 | Đóng gói | - Rà soát lỗi (Bug fixing).<br>- Sử dụng `jpackage` để đóng gói ứng dụng thành file cài đặt (`.exe`, `.dmg`).<br>- Viết tài liệu `README.md` (hướng dẫn cài Pandoc). | File cài đặt `RMS-1.0.exe` (hoặc `.dmg`). Cài đặt và chạy ứng dụng thành công. |