# Coding Convention & Architectural Guidelines
# Hệ thống Quản lý Yêu cầu (RMS)

## 1. Triết lý Cốt lõi (Core Philosophy)

1.  **Tuân thủ Kiến trúc:** Mọi dòng code đều phải tuân thủ kiến trúc **MVVM** đã chọn.
2.  **Tuân thủ SOLID:** Các nguyên tắc SOLID không phải là "tùy chọn" (optional), chúng là **bắt buộc** (mandatory).
3.  **Tách biệt (Separation of Concerns):** Logic nghiệp vụ (Services), logic UI (ViewModel), và cấu trúc UI (View) phải được tách biệt tuyệt đối.
4.  **No-Code cho BA:** BA là người dùng, không phải lập trình viên. Mọi logic (render form, render sơ đồ, xuất bản) phải được hệ thống tự động hóa. BA chỉ tương tác với GUI.

---

## 2. Cấu trúc Dự án (Project Structure - MVVM)

Toàn bộ code Java phải được tổ chức theo các package sau. Việc đặt một lớp (class) sai package bị coi là vi phạm convention.
com.rms.app ├── MainApplication.java // (Điểm khởi động JavaFX, nơi inject Guice) ├── config/ │ └── GuiceModule.java // (Cấu hình Dependency Injection) ├── model/ │ ├── Artifact.java // (POJO - Dữ liệu thô) │ ├── ArtifactTemplate.java // (POJO - Cấu hình Form) │ └── ProjectConfig.java // (POJO - Cấu hình Dự án) ├── view/ │ ├── MainView.java // (FXML Controller - "Dumb") │ ├── FormBuilderView.java // (FXML Controller - "Dumb") │ └── ... // (Các FXML Controller khác) ├── viewmodel/ │ ├── MainViewModel.java // ("Brain" - Logic UI) │ ├── ArtifactViewModel.java// ("Brain" - Logic Form) │ └── ... // (Các ViewModel khác) ├── service/ │ ├── IArtifactRepository.java // (Interface - Dùng cho DIP) │ ├── ITemplateService.java // (Interface) │ ├── IRenderService.java // (Interface) │ ├── IExportService.java // (Interface) │ └── ... // (Các Interface Service khác) ├── repository/ │ └── JsonFileRepository.java // (Implementation - Logic I/O) └── service/impl/ ├── TemplateServiceImpl.java // (Implementation - Logic nghiệp vụ) ├── RenderServiceImpl.java // (Implementation - Logic nghiệp vụ) └── ... // (Các Implementation Service khác)
---

## 3. Hướng dẫn Áp dụng SOLID (SOLID Enforcement)

### 3.1. S - Single Responsibility Principle (Nguyên tắc Trách nhiệm Đơn)
* **Quy tắc:** Một lớp chỉ nên giữ một lý do để thay đổi.
* **Áp dụng (BẮT BUỘC):**
    * **`Repository` (ví dụ: `JsonFileRepository`):** Chỉ chịu trách nhiệm ĐỌC/GHI file (`.json`, `.md`) từ ổ đĩa. KHÔNG được chứa logic nghiệp vụ.
    * **`Service` (ví dụ: `RenderService`):** Chỉ chịu trách nhiệm nghiệp vụ. Ví dụ: `RenderService` chỉ nhận dữ liệu và tạo ra code PlantUML. Nó KHÔNG được biết cách lưu file (đó là việc của `Repository`).
    * **`ViewModel` (ví dụ: `ArtifactViewModel`):** Chỉ chịu trách nhiệm về logic trình bày (presentation logic) (ví dụ: một nút "Save" có bị disable không?). Nó KHÔNG được chứa logic nghiệp vụ (thay vào đó, nó gọi `Service`).
    * **`View` (Controller):** Chỉ chịu trách nhiệm binding (liên kết) UI. Nó KHÔNG được chứa logic trình bày.

### 3.2. O - Open/Closed Principle (Nguyên tắc Đóng/Mở)
* **Quy tắc:** Đóng để sửa đổi, Mở để mở rộng.
* **Áp dụng (BẮT BUỘC):**
    * **`FormRenderer`:** Khi cần thêm một loại trường (field) mới (ví dụ: "ColorPicker"), chúng ta sẽ tạo một lớp `ColorPickerRenderer` mới (mở rộng), chứ KHÔNG sửa đổi code của `TextRenderer` hay `DropdownRenderer`.
    * **`TemplateService`:** Khi thêm một "Loại Đối tượng" mới (`@TASK`, `@REQ`...), BA chỉ cần tạo template mới (`.json`), KHÔNG cần dev sửa đổi code của `TemplateService`.

### 3.3. L - Liskov Substitution Principle (Nguyên tắc Thay thế Liskov)
* **Quy tắc:** Các lớp con (implementations) phải có thể thay thế hoàn toàn lớp cha (interface) mà không gây ra lỗi.
* **Áp dụng:** `JsonFileRepository` (implementation) phải thực thi ĐÚNG tất cả các cam kết trong `IArtifactRepository` (interface).

### 3.4. I - Interface Segregation Principle (Nguyên tắc Phân tách Interface)
* **Quy tắc:** Không ép client (lớp sử dụng) phụ thuộc vào các phương thức mà nó không dùng.
* **Áp dụng:** Tạo các interface nhỏ, chuyên biệt. Ví dụ:
    * `IExportService` (có hàm `exportToPdf`)
    * `IImportService` (có hàm `importFromExcel`)
    * ... thay vì một `IFileService` khổng lồ.

### 3.5. D - Dependency Inversion Principle (Nguyên tắc Đảo ngược Phụ thuộc)
* **Quy tắc:** Module cấp cao (ViewModel) không nên phụ thuộc vào module cấp thấp (Repository). Cả hai nên phụ thuộc vào TRỪU TƯỢNG (Interface).
* **Áp dụng (QUAN TRỌNG NHẤT):**
    * **PHẢI** sử dụng **Dependency Injection** (ví dụ: **Google Guice**).
    * **CẤM (FORBIDDEN):** Không bao giờ được dùng từ khóa `new` để khởi tạo một Service hay Repository.
        ```java
        // CẤM (VI PHẠM DIP):
        public class ArtifactViewModel {
            private IArtifactRepository repo = new JsonFileRepository(); // CẤM!
        }
        ```
    * **BẮT BUỘC (CORRECT):** Sử dụng `@Inject` của Guice.
        ```java
        // BẮT BUỘC (TUÂN THỦ DIP):
        public class ArtifactViewModel {
            private final IArtifactRepository repository;
            private final IRenderService renderService;

            @Inject
            public ArtifactViewModel(IArtifactRepository repository, IRenderService renderService) {
                this.repository = repository;
                this.renderService = renderService;
            }
        }
        ```

---

## 4. Quy ước Code Chi tiết (Detailed Conventions)

### 4.1. Quy ước Đặt tên (Naming)
* **Packages:** `com.rms.app.viewmodel` (chữ thường).
* **Classes (Lớp):** PascalCase (ví dụ: `ArtifactViewModel`, `RenderServiceImpl`).
* **Interfaces (Giao diện):** PascalCase, khuyến khích dùng tiền tố `I` (ví dụ: `IArtifactRepository`).
* **Methods (Hàm):** camelCase (ví dụ: `loadProject()`).
* **Variables (Biến):** camelCase (ví dụ: `currentArtifact`).
    * **JavaFX Properties (trong ViewModel):** camelCase + hậu tố "Property" (ví dụ: `nameProperty`, `stepListProperty`).
* **Constants (Hằng số):** `UPPER_SNAKE_CASE` (ví dụ: `DEFAULT_TIMEOUT = 2000`).
* **FXML Files:** PascalCase, khớp với View Controller (ví dụ: `MainView.fxml`, `FormBuilderView.fxml`).
* **CSS Files:** kebab-case (ví dụ: `dark-theme.css`).
* **Data Files (Project):**
    * **Source of Truth:** `[ID].json` (ví dụ: `UC001.json`).
    * **Git Mirror:** `[ID].md` (ví dụ: `UC001.md`).
    * **Template:** `[TemplateName].template.json` (ví dụ: `uc.template.json`).

### 4.2. Hướng dẫn MVVM (MVVM Enforcement)
* **Lớp Model (POJO):**
    * **CẤM:** Không bao giờ được `import javafx.*`.
    * PHẢI sử dụng các kiểu dữ liệu Java gốc (String, List, int...).
    * PHẢI sử dụng annotation của Jackson (`@JsonProperty`) để mapping.
* **Lớp ViewModel:**
    * **CẤM:** Không bao giờ được `import javafx.scene.control.*` (ví dụ: `TextField`, `Button`).
    * **PHẢI:** Sử dụng JavaFX Properties (`StringProperty`, `ObjectProperty`, `ObservableList`).
    * PHẢI phơi bày (expose) các `Property` (ví dụ: `public StringProperty nameProperty()`) để View bind vào.
    * PHẢI chứa logic xác thực (validation) và logic trạng thái UI.
    * PHẢI `@Inject` và gọi các `Service` để thực hiện nghiệp vụ.
* **Lớp View (Controller):**
    * **CẤM:** Không bao giờ được chứa logic nghiệp vụ (if/else...).
    * **PHẢI:** Sử dụng `@FXML` để inject các control từ FXML.
    * **PHẢI:** `@Inject` ViewModel tương ứng.
    * **PHẢI:** Trong hàm `initialize()`, thực hiện binding (ví dụ: `textField.textProperty().bindBidirectional(viewModel.nameProperty())`).

### 4.3. Đa luồng (Concurrency)
* **CẤM (FORBIDDEN):** Không bao giờ thực hiện I/O (lưu/tải file), gọi API (Gemini), hay render (PlantUML) trên **JavaFX Application Thread**.
* **BẮT BUỘC (MANDATORY):**
    * PHẢI sử dụng `javafx.concurrent.Task` hoặc `javafx.concurrent.Service` cho mọi tác vụ nặng.
    * PHẢI sử dụng `Platform.runLater(...)` để cập nhật UI từ một luồng nền.
    * PHẢI sử dụng `PauseTransition` cho logic Auto-save (để tránh I/O trên mỗi phím gõ).

### 4.4. Logging
* **CẤM:** Không sử dụng `System.out.println()`.
* **BẮT BUỘC:** Sử dụng thư viện logging (ví dụ: **SLF4J + Logback**).

### 4.5. Xử lý Ngoại lệ (Exception Handling)
* **CẤM:** Không "nuốt" (swallow) exception (ví dụ: `catch (Exception e) {}`).
* **BẮT BUỘC:** Phải log (ghi lại) exception.
* **BẮT BUỘC:** ViewModel phải bắt các exception từ Service và phơi bày (expose) thông báo lỗi cho View (ví dụ: qua một `StringProperty errorProperty`) để hiển thị cho người dùng.