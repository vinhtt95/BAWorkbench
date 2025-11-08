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

## Cài đặt Môi trường (Bắt buộc)

Bạn **phải** cài đặt các công cụ sau và thêm chúng vào `PATH` hệ thống để ứng dụng hoạt động đầy đủ.

### 1. Java JDK 21+
* **Mục đích:** Cần thiết để chạy ứng dụng.
* **Tải về:** Bạn có thể tải **OpenJDK** (khuyến nghị) hoặc Oracle JDK từ trang chủ chính thức.

### 2. Pandoc
* **Mục đích:** Cần thiết cho chức năng "Export to Document (PDF/DOCX)" (UC-PUB-01).
* **Windows (Khuyến nghị: dùng Chocolatey):**
    ```bash
    choco install pandoc
    ```
  (Hoặc tải file `.msi` từ [trang chủ Pandoc](https://pandoc.org/installing.html))
* **macOS (Khuyến nghị: dùng Homebrew):**
    ```bash
    brew install pandoc
    ```
* **Linux (Ubuntu/Debian):**
    ```bash
    sudo apt-get update
    sudo apt-get install pandoc
    ```

### 3. XeLaTeX (MiKTeX / BasicTeX)
* **Mục đích:** Cần thiết cho Pandoc để tạo file PDF hỗ trợ Unicode (Tiếng Việt).
* **Windows (Khuyến nghị: MiKTeX):**
    ```bash
    choco install miktex
    ```
  *(Lưu ý: Sau khi cài, bạn có thể cần chạy MiKTeX Console một lần để nó hoàn tất cài đặt và tự động tải các gói (package) khi cần).*
* **macOS (Khuyến nghị: BasicTeX):**
    ```bash
    brew install --cask basictex
    ```
    ```bash
    echo 'export PATH="/Library/TeX/texbin:$PATH"' >> ~/.zshrc
    source ~/.zshrc
    ```
  * (Lệnh này tạo một file mới tên là TeX trong thư mục /etc/paths.d/ và đặt đường dẫn /Library/TeX/texbin vào đó. Đây là cách chuẩn của macOS để thêm PATH cho mọi ứng dụng).
    ```bash
    echo "/Library/TeX/texbin" | sudo tee /etc/paths.d/TeX
    ```
  * (BasicTeX là bản cài đặt nhỏ. Nếu gặp lỗi thiếu gói (package), bạn có thể cài bản đầy đủ: `brew install --cask mactex`)*.
* **Linux (Ubuntu/Debian):**
    ```bash
    sudo apt-get update
    sudo apt-get install texlive-xetex
    ```
  *(Bạn cũng có thể cần `texlive-lang-vietnamese` để hỗ trợ Tiếng Việt đầy đủ).*

### 4. Kiểm tra Cài đặt
Sau khi cài đặt, **khởi động lại** Terminal (hoặc Command Prompt/PowerShell) và chạy các lệnh sau để xác nhận:

```bash
java --version
pandoc --version
pdflatex --version