# Coding Convention & Architectural Guidelines
# Hệ thống Quản lý Yêu cầu (RMS)

## 1. Triết lý Cốt lõi (Core Philosophy)

1.  **Tuân thủ Kiến trúc:** Mọi dòng code đều phải tuân thủ kiến trúc **MVVM** đã chọn.
2.  **Tuân thủ SOLID:** Các nguyên tắc SOLID không phải là "tùy chọn", chúng là **bắt buộc**.
3.  **Tách biệt (Separation of Concerns):** Logic nghiệp vụ, logic UI, và cấu trúc UI phải được tách biệt tuyệt đối.
4.  **Kiến trúc Dữ liệu:** Dữ liệu được tổ chức theo 3 lớp: **JSON** (Source of Truth), **MD** (Git Mirror), và **SQLite** (Index/Cache).

---

## 2. Cấu trúc Dự án (Project Structure - MVVM)

Toàn bộ code Java phải được tổ chức theo các package sau. Việc đặt một lớp (class) sai package bị coi là vi phạm convention.

com.rms.app ├── MainApplication.java // (Điểm khởi động JavaFX, nơi inject Guice) ├── config/ │ └── GuiceModule.java // (Cấu hình Dependency Injection) ├── model/ │ ├── Artifact.java // (POJO - Dữ liệu thô) │ ├── ... // (Các POJO khác) ├── view/ │ ├── MainView.java // (FXML Controller - "Dumb") │ └── ... // (Các FXML Controller khác) ├── viewmodel/ │ ├── MainViewModel.java // ("Brain" - Logic UI) │ ├── ArtifactViewModel.java// ("Brain" - Logic Form) │ └── ... // (Các ViewModel khác) ├── service/ │ ├── IArtifactRepository.java // (Interface - Dùng cho DIP) │ ├── ITemplateService.java // (Interface) │ ├── IRenderService.java // (Interface) │ ├── IIndexService.java // (MỚI - Interface cho Index) │ └── ... // (Các Interface Service khác) ├── repository/ │ └── JsonFileRepository.java // (Implementation - Logic I/O file) │ └── SqliteIndexRepository.java // (MỚI - Logic I/O CSDL) └── service/impl/ ├── TemplateServiceImpl.java // (Implementation - Logic nghiệp vụ) ├── RenderServiceImpl.java // (Implementation - Logic nghiệp vụ) ├── IndexServiceImpl.java // (MỚI - Logic nghiệp vụ Index) └── ... // (Các Implementation Service khác)

(Cấu trúc file dự án trên ổ đĩa) /MyProjectName/ ├── .config/ │ ├── project.json │ ├── uc.template.json │ └── index.db (MỚI - File SQLite Index) ├── Artifacts/ │ ├── UseCases/ │ │ ├── UC001.json (Source of Truth) │ │ └── UC001.md (Git Mirror) │ └── ... └── .gitignore (PHẢI bao gồm: .config/index.db, .cache/)

---

## 3. Hướng dẫn Áp dụng SOLID (SOLID Enforcement)

### 3.1. S - Single Responsibility Principle (Nguyên tắc Trách nhiệm Đơn)
* **`Repository` (ví dụ: `JsonFileRepository`, `SqliteIndexRepository`):** Chỉ chịu trách nhiệm ĐỌC/GHI dữ liệu thô (file hoặc DB). KHÔNG chứa logic nghiệp vụ (ví dụ: không check link).
* **`Service` (ví dụ: `IndexService`):** Chứa logic nghiệp vụ. Ví dụ: `IndexService` điều phối việc quét file (gọi `JsonFileRepository.loadAll()`) và ghi vào DB (gọi `SqliteIndexRepository.saveIndex()`).
* **`ViewModel`:** Chỉ chịu trách nhiệm logic trình bày.
* **`View`:** Chỉ chịu trách nhiệm binding UI.

### 3.2. O - Open/Closed Principle (Nguyên tắc Đóng/Mở)
* **`FormRenderer`:** Khi thêm loại trường mới, chúng ta tạo lớp Renderer mới (mở rộng), không sửa code cũ.
* **`TemplateService`:** Khi BA thêm "Loại Đối tượng" mới, không cần sửa code.

### 3.3. L - Liskov Substitution Principle (Nguyên tắc Thay thế Liskov)
* `JsonFileRepository` phải thực thi đúng cam kết của `IArtifactRepository`.

### 3.4. I - Interface Segregation Principle (Nguyên tắc Phân tách Interface)
* Tạo các interface nhỏ: `IExportService`, `IImportService`, `IIndexService` (thay vì một `IUtilityService` khổng lồ).

### 3.5. D - Dependency Inversion Principle (Nguyên tắc Đảo ngược Phụ thuộc)
* **BẮT BUỘC:** Sử dụng **Dependency Injection** (Guice) cho tất cả Services và Repositories.
* **CẤM (FORBIDDEN):** Không bao giờ dùng `new` để khởi tạo Service/Repository.
* **Ví dụ (Cập nhật):**
    ```java
    // (Trong lớp Repository)
    public class JsonFileRepository implements IArtifactRepository {
        private final IIndexService indexService; // (MỚI)

        @Inject
        public JsonFileRepository(IIndexService indexService) {
            this.indexService = indexService;
        }

        @Override
        public void save(Artifact artifact) {
            // 1. Ghi file .json
            // ...
            // 2. Ghi file .md
            // ...
            // 3. Cập nhật Index (gọi Service khác)
            this.indexService.updateArtifactInIndex(artifact); 
        }

        @Override
        public void delete(String id) {
            // 1. Kiểm tra toàn vẹn (gọi Service)
            if (this.indexService.hasBacklinks(id)) {
                throw new IntegrityViolationException("Artifact has backlinks.");
            }
            // 2. Xóa file .json, .md
            // ...
            // 3. Xóa khỏi Index (gọi Service)
            this.indexService.deleteArtifactFromIndex(id);
        }
    }
    ```

---

## 4. Quy ước Code Chi tiết (Detailed Conventions)

### 4.1. Quy ước Đặt tên (Naming)
(Không thay đổi, ngoại trừ các file/lớp mới)
* **Classes:** `IndexServiceImpl`, `SqliteIndexRepository`
* **Data Files (Project):** `/.config/index.db` (PHẢI nằm trong `.gitignore`)

### 4.2. Hướng dẫn MVVM (MVVM Enforcement)
(Không thay đổi)

### 4.3. Đa luồng (Concurrency)
* **CẤM (FORBIDDEN):** Không bao giờ thực hiện I/O (lưu/tải file), gọi API (Gemini), render (PlantUML), hoặc **truy vấn/xây dựng CSDL (SQLite)** trên **JavaFX Application Thread**.
* **BẮT BUỘC (MANDATORY):**
    * PHẢI sử dụng `javafx.concurrent.Task` hoặc `javafx.concurrent.Service` cho mọi tác vụ nặng (bao gồm `IndexService.validateAndRebuildIndex()`).
    * PHẢI sử dụng `Platform.runLater(...)` để cập nhật UI từ một luồng nền.
    * PHẢI sử dụng `PauseTransition` cho logic Auto-save.

### 4.4. Logging
* **CẤM:** Không dùng `System.out.println()`.
* **BẮT BUỘC:** Sử dụng thư viện logging (ví dụ: **SLF4J + Logback**).

### 4.5. Xử lý Ngoại lệ (Exception Handling)
* **CẤM:** Không "nuốt" (swallow) exception (ví dụ: `catch (Exception e) {}`).
* **BẮT BUỘC:** Phải log (ghi lại) exception. (Đặc biệt là `SQLException` từ SQLite).
* **BẮT BUỘC:** ViewModel phải bắt các exception từ Service (ví dụ: `IntegrityViolationException` khi xóa) và hiển thị cảnh báo rõ ràng cho BA.
### 4.6. Quy ước Comment (JavaDoc)
* **BẮT BUỘC:** Chỉ sử dụng comment Javadoc (`/** ... */`) cho tất cả các bình luận (comment) trong mã nguồn.
* **CẤM:** Tuyệt đối không sử dụng comment một dòng (`//`) hoặc comment khối (`/* ... */`).
* **Quy tắc cho Javadoc:**
    * **Class/Interface:** Mọi public class/interface PHẢI có Javadoc mô tả mục đích của lớp/interface đó.
    * **Public Methods:** Mọi public method PHẢI có Javadoc mô tả chức năng, và PHẢI bao gồm các tag sau nếu có:
        * `@param [tên_biến] [Mô tả]` - (Dùng cho mọi tham số đầu vào).
        * `@return [Mô tả]` - (Mô tả giá trị trả về, trừ khi hàm là `void`).
        * `@throws [Tên_Exception] [Lý do]` - (Mô tả các ngoại lệ (checked) có thể bị ném ra).
    * **Private Methods:** Khuyến khích (Khả năng bảo trì - QA-01) viết Javadoc đơn giản (mô tả chức năng) cho các private method phức tạp để làm rõ logic nghiệp vụ.

* **Ví dụ (Bắt buộc):**
    ```java
    /**
     * Lưu một đối tượng (Artifact) vào file system.
     *
     * @param artifact Đối tượng cần lưu (không được null).
     * @param overwrite Cờ (flag) cho biết có ghi đè file đã tồn tại hay không.
     * @return ID của đối tượng đã lưu.
     * @throws IOException Nếu xảy ra lỗi ghi file.
     */
    public String save(Artifact artifact, boolean overwrite) throws IOException {
        // ... logic ...
    }
    ```