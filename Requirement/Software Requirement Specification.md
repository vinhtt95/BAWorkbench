# Software Requirements Specification
cho
Hệ thống Quản lý Yêu cầu (Requirements Management System - RMS)
Phiên bản 1.0

Ngày 06 tháng 11 năm 2025

# Table of Contents
1. [Introduction](#1-introduction)
    1. [Purpose](#11-purpose)
    2. [Document Conventions](#12-document-conventions)
    3. [Intended Audience](#13-intended-audience)
    4. [Product Scope](#14-product-scope)
    5. [References](#15-references)
2. [Overall Description](#2-overall-description)
    1. [Product Perspective](#21-product-perspective)
    2. [Product Features](#22-product-features)
    3. [User Classes and Characteristics](#23-user-classes-and-characteristics)
    4. [Operating Environment](#24-operating-environment)
    5. [Design and Implementation Constraints](#25-design-and-implementation-constraints)
    6. [Assumptions and Dependencies](#26-assumptions-and-dependencies)
3. [System Features (Functional Requirements)](#3-system-features)
   3.1. [Gói Quản lý Dự án (UC-PKG-PM)](#31-project-management)
   3.2. [Gói Cấu hình Dự án (UC-PKG-CFG)](#32-project-configuration)
   3.3. [Gói Phát triển Yêu cầu (UC-PKG-DEV)](#33-requirement-development)
   3.4. [Gói Mô hình hóa Yêu cầu (UC-PKG-MOD)](#34-requirement-modeling)
   3.5. [Gói Quản lý Yêu cầu (UC-PKG-MGT)](#35-requirement-management)
   3.6. [Gói Xuất bản Tài liệu (UC-PKG-PUB)](#36-publication)
4. [External Interface Requirements](#4-external-interface-requirements)
    1. [User Interfaces](#41-user-interfaces)
    2. [Hardware Interfaces](#42-hardware-interfaces)
    3. [Software Interfaces](#43-software-interfaces)
    4. [Communications Interfaces](#44-communications-interfaces)
5. [Nonfunctional Requirements](#5-nonfunctional-requirements)
    1. [Performance Requirements](#51-performance-requirements)
    2. [Security Requirements](#52-security-requirements)
    3. [Software Quality Attributes](#53-software-quality-attributes)
6. [Other Requirements](#6-other-requirements)

Appendix A: Glossary
Appendix B: Analysis Models (Use Cases)

---

# 1. Introduction

## 1.1 Purpose
Tài liệu này đặc tả các yêu cầu cho **Hệ thống Quản lý Yêu cầu (RMS)**, Phiên bản 1.0.

Mục đích của hệ thống RMS là cung cấp cho các Chuyên gia Phân tích Nghiệp vụ (Business Analysts - BA) một công cụ desktop mạnh mẽ, tích hợp để thay thế quy trình quản lý yêu cầu thủ công, rời rạc bằng các file Word, Excel và Visio. Hệ thống sẽ chuyển đổi các tài liệu tĩnh thành một cơ sở tri thức (knowledge base) sống động, có cấu trúc, có quan hệ (relational) và được mô hình hóa (model-driven). RMS sẽ quản lý toàn bộ vòng đời của yêu cầu, từ việc khởi tạo, cấu hình, phát triển (nhập liệu qua form), mô hình hóa (tự động sinh sơ đồ) đến quản lý (theo dõi tiến độ, phiên bản) và xuất bản (PDF/DOCX/Excel).

## 1.2 Document Conventions
Tài liệu này tuân theo mẫu SRS chuẩn.
* **REQ-XXX-NN:** Định danh cho một yêu cầu chức năng cụ thể.
* **UC-XXX-NN:** Định danh cho một Use Case, tham chiếu đến tài liệu Use Case chi tiết (xem Phụ lục B).
* **@ID:** Cú pháp chuẩn để tham chiếu chéo (cross-reference) giữa các Đối tượng Yêu cầu (Artifacts) trong hệ thống.
* **NFR-NN:** Định danh cho một yêu cầu phi chức năng.

## 1.3 Intended Audience
* **Business Analyst (BA):** Người dùng chính, sử dụng hệ thống hàng ngày để tạo, quản lý và mô hình hóa yêu cầu.
* **Project Manager (PM):** Sử dụng các tính năng Dashboard (Kanban/Gantt) và Quản lý Phiên bản (Release) để theo dõi tiến độ và phạm vi (scope) dự án.
* **Development & QA Teams:** Người tiêu thụ (consumer) các tài liệu (PDF/DOCX) được xuất bản từ hệ thống.
* **System Architect/Developer (Người phát triển RMS):** Sử dụng tài liệu này để thiết kế và xây dựng ứng dụng.

## 1.4 Product Scope
Hệ thống RMS là một ứng dụng desktop tập trung vào việc cho phép BA tự định nghĩa cấu trúc dự án của mình và nhập liệu yêu cầu thông qua các Form trực quan (No-code).

**Các chức năng chính (In-Scope):**
* Quản lý dự án (Tạo, Mở, Import/Export Excel).
* Tùy chỉnh dự án "no-code" (Form Builder, Template Builder).
* Nhập liệu yêu cầu qua Form (Form-driven), bao gồm cả trình tạo luồng (Flow Builder) trực quan.
* Tự động mô hình hóa (Auto-modeling) dữ liệu Form thành sơ đồ PlantUML (Activity Diagram, BPMN).
* Truy vết yêu cầu thông minh (Linking, Backlinks, Graph View).
* Quản lý tiến độ (Kanban, Gantt) và phiên bản (Release).
* Tích hợp AI (Gemini) để hỗ trợ viết yêu cầu.
* Tích hợp Figma (Link) để liên kết với thiết kế.
* Xuất bản tài liệu chuyên nghiệp (PDF/DOCX) theo template tùy chỉnh.

**Các chức năng ngoài phạm vi (Out-of-Scope) cho v1.0:**
* Hợp tác thời gian thực (Real-time collaboration).
* Phiên bản Web/Cloud (Đây là ứng dụng desktop, local-first).
* Tích hợp trực tiếp với các hệ thống ALM (Jira, DevOps) - có thể xem xét ở phiên bản sau.

## 1.5 References
* [Overview.md](UseCases/Overview.md) (Danh sách Use Case tổng thể)
* [UC-PM-01.md](UseCases/UC-PM-01.md) (Tạo dự án mới)
* [UC-PM-02.md](UseCases/UC-PM-02.md) (Mở dự án có sẵn)
* [UC-PM-03.md](UseCases/UC-PM-03.md) (Import dự án từ Excel)
* [UC-CFG-01.md](UseCases/UC-CFG-01.md) (Quản lý Loại Đối tượng)
* [UC-CFG-02.md](UseCases/UC-CFG-02.md) (Quản lý Phiên bản)
* [UC-CFG-03.md](UseCases/UC-CFG-03.md) (Quản lý Template Xuất bản)
* [UC-DEV-01.md](UseCases/UC-DEV-01.md) (Tạo/Sửa Đối tượng Yêu cầu)
* [UC-DEV-02.md](UseCases/UC-DEV-02.md) (Liên kết các Đối tượng)
* [UC-MOD-01.md](UseCases/UC-MOD-01.md) (Xem Sơ đồ tự động sinh)
* [UC-MOD-02.md](UseCases/UC-MOD-02.md) (Xem Sơ đồ Quan hệ)
* [UC-MGT-01.md](UseCases/UC-MGT-01.md) (Quản lý Tác vụ)
* [UC-MGT-02.md](UseCases/UC-MGT-02.md) (Theo dõi tiến độ)
* [UC-MGT-03.md](UseCases/UC-MGT-03.md) (Gán Yêu cầu vào Phiên bản)
* [UC-PUB-01.md](UseCases/UC-PUB-01.md) (Xuất bản tài liệu)
* [UC-PUB-02.md](UseCases/UC-PUB-02.md) (Xuất dự án ra Excel)

# 2. Overall Description

## 2.1 Product Perspective
Hệ thống RMS là một sản phẩm phần mềm mới, độc lập (standalone). Nó được thiết kế để trở thành công cụ chính trong bộ công cụ (toolchain) của BA. RMS hoạt động dựa trên hệ thống file local, đảm bảo 100% tương thích với các hệ thống Quản lý Phiên bản (VCS) như Git. Mọi thay đổi trong yêu cầu đều được phản ánh dưới dạng thay đổi (diff) rõ ràng trong các file `.json` (source of truth) và `.md` (git-friendly mirror), cho phép truy vết lịch sử thay đổi đầy đủ.

## 2.2 Product Features
Các chức năng chính của hệ thống bao gồm:
1.  **Quản lý Dự án (PM):** Cho phép BA tạo, mở và import/export dự án (Excel).
2.  **Cấu hình Dự án (CFG):** Cung cấp các công cụ "no-code" (Form Builder) để BA tự định nghĩa cấu trúc dữ liệu (Artifact Types) và cấu trúc xuất bản (Export Templates) của mình.
3.  **Phát triển Yêu cầu (DEV):** Cung cấp giao diện Form trực quan (được sinh tự động) để BA nhập liệu yêu cầu, bao gồm `Flow Builder` để định nghĩa logic quy trình.
4.  **Mô hình hóa Yêu cầu (MOD):** Tự động sinh sơ đồ (PlantUML) từ dữ liệu Form, và cung cấp các công cụ phân tích trực quan (Graph View, Backlinks).
5.  **Quản lý Yêu cầu (MGT):** Cung cấp các công cụ để theo dõi tiến độ (Kanban, Gantt) và quản lý phạm vi (Release Versioning).
6.  **Xuất bản Tài liệu (PUB):** Cho phép BA xuất bản các tài liệu SRS chuyên nghiệp (PDF/DOCX) dựa trên template tùy chỉnh.

## 2.3 User Classes and Characteristics
| Lớp Người dùng | Đặc điểm | Chức năng chính sử dụng |
| :--- | :--- | :--- |
| **Business Analyst (BA)** | (Người dùng chính) Có kiến thức về quy trình nghiệp vụ và đặc tả yêu cầu. Không phải là lập trình viên. | Tất cả các chức năng. Trọng tâm là CFG, DEV, MOD, MGT. |
| **Project Manager (PM)** | Cần theo dõi tiến độ và phạm vi dự án ở mức cao. | MGT (Kanban/Gantt, Release), PUB (Đọc tài liệu). |
| **Developer / QA** | Cần đọc và hiểu các yêu cầu đã được phê duyệt. | PUB (Đọc tài liệu PDF/DOCX được xuất bản). |

## 2.4 Operating Environment
* **Hệ điều hành:** Đa nền tảng (Windows 10/11, macOS 13+, Linux).
* **Runtime:** Java Runtime Environment (JRE/JDK) 17+ và JavaFX Runtime 17+.
* **Tài nguyên:** Tối thiểu 4GB RAM, 500MB dung lượng ổ đĩa (không bao gồm các dự án).

## 2.5 Design and Implementation Constraints
* **C-01 (Ngôn ngữ):** Hệ thống PHẢI được phát triển bằng ngôn ngữ Java (JDK 17+).
* **C-02 (UI Framework):** Giao diện người dùng PHẢI được xây dựng bằng JavaFX.
* **C-03 (Kiến trúc):** PHẢI tuân thủ chặt chẽ mẫu kiến trúc MVVM (Model-View-ViewModel) và các nguyên tắc SOLID.
* **C-04 (Lưu trữ):** Dữ liệu dự án PHẢI được lưu trữ trên hệ thống file local (Local-first). Không được phụ thuộc vào cơ sở dữ liệu (SQL, NoSQL) bên ngoài.
* **C-05 (Định dạng "Source of Truth"):** Dữ liệu nhập từ Form PHẢI được lưu dưới dạng file `.json` (hoặc `.yaml`). Đây là nguồn sự thật (Source of Truth).
* **C-06 (Định dạng Git-Friendly):** Một file `.md` (Markdown) PHẢI được tự động sinh ra (auto-generate) từ file `.json` trên mỗi lần lưu, chỉ nhằm mục đích cung cấp "diff" rõ ràng cho Git.
* **C-07 (Tương tác):** Mọi thao tác cấu hình (Form Builder, Export Template) PHẢI thực hiện qua GUI (No-code).
* **C-08 (Thư viện Sơ đồ):** PHẢI sử dụng thư viện PlantUML (bản Java `.jar`) để render sơ đồ (không dùng Mermaid.js).
* **C-09 (Thư viện Xuất bản):** PHẢI dựa vào công cụ **Pandoc** (cài đặt bên ngoài) để chuyển đổi từ Markdown trung gian sang PDF/DOCX.

## 2.6 Assumptions and Dependencies
* **A-01:** Người dùng (BA) có kiến thức cơ bản về Git để quản lý phiên bản thư mục dự án.
* **A-02:** Người dùng đã cài đặt **Pandoc** và cấu hình nó trong PATH hệ thống để sử dụng chức năng Xuất bản (UC-PUB-01).
* **A-03:** Người dùng có quyền đọc/ghi trên thư mục mà họ chọn để tạo/mở dự án.
* **A-04:** Người dùng có kết nối Internet đang hoạt động để sử dụng các tính năng API (Gemini, Figma).

---

# 3. System Features (Functional Requirements)

Đây là danh sách chi tiết các yêu cầu chức năng, được nhóm theo các gói tính năng.

## 3.1 Project Management (UC-PKG-PM)
* **REQ-PM-01:** Hệ thống PHẢI cho phép người dùng tạo một dự án RMS mới tại một vị trí thư mục trống do người dùng chỉ định (tham chiếu `UC-PM-01`).
* **REQ-PM-02:** Hệ thống PHẢI cho phép người dùng mở một dự án RMS đã tồn tại bằng cách chọn thư mục gốc của dự án đó (tham chiếu `UC-PM-02`).
* **REQ-PM-03:** Hệ thống PHẢI cung cấp tính năng "Import from Excel" (tham chiếu `UC-PM-03`).
* **REQ-PM-04:** Tính năng Import PHẢI cung cấp một giao diện GUI cho phép BA ánh xạ (map) các "Sheet" trong Excel với các "Loại Đối tượng" (Artifact Types) trong RMS.
* **REQ-PM-05:** Tính năng Import PHẢI cho phép BA ánh xạ (map) các "Cột" (Columns) trong Sheet với các "Trường" (Fields) trong Form của Loại Đối tượng tương ứng.

## 3.2 Project Configuration (UC-PKG-CFG)
* **REQ-CFG-01:** Hệ thống PHẢI cung cấp một "Trình thiết kế Form" (Form Builder) trực quan (GUI) cho phép BA tạo, sửa, xóa các "Loại Đối tượng" (Artifact Types) (tham chiếu `UC-CFG-01`).
* **REQ-CFG-02:** "Trình thiết kế Form" PHẢI cung cấp một "Toolbox" các loại trường có thể kéo-thả, bao gồm:
    * `Text (Single-line)`
    * `Text Area (Multi-line)`
    * `Dropdown (Nguồn tĩnh hoặc động)`
    * `Linker (Dùng để liên kết @ID)`
    * `Flow Builder (Thành phần GUI đặc biệt để xây dựng luồng)`
* **REQ-CFG-03:** BA PHẢI có khả năng gán một "Tiền tố ID" (Prefix ID) duy nhất (ví dụ: "UC", "BR") cho mỗi Loại Đối tượng.
* **REQ-CFG-04:** Cấu hình Form Builder PHẢI được lưu dưới dạng file `.json` (hoặc `.yaml`) trong thư mục `.config` của dự án.
* **REQ-CFG-05:** Hệ thống PHẢI cung cấp giao diện GUI cho phép BA tạo, sửa, xóa các "Phiên bản" (Releases) của dự án (tham chiếu `UC-CFG-02`).
* **REQ-CFG-06:** Hệ thống PHẢI cung cấp một "Trình thiết kế Template Xuất bản" (GUI) cho phép BA định nghĩa cấu trúc file PDF/DOCX (tham chiếu `UC-CFG-03`).
* **REQ-CFG-07:** "Trình thiết kế Template Xuất bản" PHẢI hỗ trợ 2 loại "Chương" (Section):
    * "Chương Tĩnh" (Nội dung Markdown do BA tự gõ).
    * "Chương Động" (Nội dung được tự động truy vấn từ các đối tượng, ví dụ: "Lấy tất cả @UC có Status=Approved").
* **REQ-CFG-08:** "Chương Động" PHẢI cho phép BA tùy chỉnh cách hiển thị (Dạng `Bảng` hoặc Dạng `Nội dung đầy đủ`).
* **REQ-CFG-09:** Hệ thống PHẢI cung cấp khu vực "Settings" để BA nhập API Key cho Gemini và Figma.

## 3.3 Requirement Development (UC-PKG-DEV)
* **REQ-DEV-01:** Khi BA tạo/sửa một đối tượng, hệ thống PHẢI tự động sinh (render) ra "Form View" dựa trên template đã cấu hình trong `UC-CFG-01`. "Form View" là giao diện mặc định để chỉnh sửa (tham chiếu `UC-DEV-01`).
* **REQ-DEV-02:** Thành phần `Flow Builder` (GUI) PHẢI cho phép BA thêm/xóa/sắp xếp lại các bước (steps) bằng cách kéo-thả.
* **REQ-DEV-03:** `Flow Builder` PHẢI cho phép BA định nghĩa logic (ví dụ: `If-Then-Else`, `Loop`) và chọn Tác nhân (Actor) cho từng bước mà không cần viết code.
* **REQ-DEV-04:** Hệ thống PHẢI hỗ trợ cú pháp `@ID` trong các trường text (ví dụ: `TextArea`, `Linker`) để tạo liên kết (tham chiếu `UC-DEV-02`).
* **REQ-DEV-05:** Khi BA gõ `@`, hệ thống PHẢI hiển thị một cửa sổ (popup) tự động hoàn thành (autocomplete) để tìm kiếm tất cả các đối tượng trong dự án.
* **REQ-DEV-06:** Khi BA di chuột (hover) qua một tag `@ID`, hệ thống PHẢI hiển thị một "Hover Card" (tooltip) tóm tắt thông tin của đối tượng đó.
* **REQ-DEV-07:** Khi BA click (hoặc Ctrl+Click) vào một tag `@ID`, hệ thống PHẢI mở đối tượng đó trong một Tab mới.
* **REQ-DEV-08:** Hệ thống PHẢI tích hợp API Gemini, cung cấp một nút (ví dụ: "Gemini: Đề xuất") bên cạnh các trường (như `Flow Builder`) để tự động điền nội dung đề xuất vào Form (tham chiếu `UC-DEV-01`, Luồng 1.0.A1).
* **REQ-DEV-09:** Hệ thống PHẢI cho phép BA dán (paste) một URL Figma vào một trường "Figma Link". Hệ thống sẽ hiển thị URL này dưới dạng hyperlink (tham chiếu `UC-DEV-04` trong Overview).

## 3.4 Requirement Modeling (UC-PKG-MOD)
* **REQ-MOD-01:** Hệ thống PHẢI cung cấp một tab "Diagram View" (Chế độ xem Sơ đồ) cho các đối tượng có `Flow Builder` (tham chiếu `UC-MOD-01`).
* **REQ-MOD-02:** "Diagram View" PHẢI tự động sinh (auto-generate) một sơ đồ PlantUML (ví dụ: Sơ đồ Hoạt động) từ dữ liệu đã nhập trong `Flow Builder`.
* **REQ-MOD-03:** "Diagram View" PHẢI ở chế độ Read-only. Mọi chỉnh sửa phải được thực hiện từ "Form View".
* **REQ-MOD-04:** Sơ đồ trong "Diagram View" PHẢI tự động cập nhật (hoặc có nút Refresh) khi dữ liệu trong "Form View" thay đổi và được lưu.
* **REQ-MOD-05:** Hệ thống PHẢI cung cấp một tab "Project Graph" (Sơ đồ Quan hệ) toàn cục, hiển thị tất cả các đối tượng (nodes) và các liên kết `@ID` (edges) (tham chiếu `UC-MOD-02`).
* **REQ-MOD-06:** "Project Graph" PHẢI có tính tương tác: cho phép zoom/pan, và cho phép double-click vào một "node" để mở đối tượng đó.
* **REQ-MOD-07:** Hệ thống PHẢI cung cấp một tab/cột "Backlinks" (Liên kết ngược) cho mỗi đối tượng, hiển thị danh sách tất cả các đối tượng khác đang liên kết (tag `@ID`) đến nó.

## 3.5 Requirement Management (UC-PKG-MGT)
* **REQ-MGT-01:** Hệ thống PHẢI hỗ trợ một "Loại Đối tượng" là "Task" (`@TSK`) để BA lập kế hoạch công việc (tham chiếu `UC-MGT-01`).
* **REQ-MGT-02:** Template `@TSK` PHẢI có các trường tối thiểu: `Status` (Trạng thái) và `Due Date` (Ngày hết hạn).
* **REQ-MGT-03:** Hệ thống PHẢI cung cấp một "Planning Dashboard" với chế độ xem "Kanban View" (tham chiếu `UC-MGT-02`).
* **REQ-MGT-04:** "Kanban View" PHẢI tự động đọc tất cả các đối tượng (<code>@UC</code>, <code>@BR</code>...) và sắp xếp chúng vào các cột dựa trên trường "Status" (ví dụ: "Draft", "In Review", "Approved").
* **REQ-MGT-05:** BA PHẢI có thể kéo-thả các thẻ (cards) trong "Kanban View" từ cột này sang cột khác để cập nhật "Status" của đối tượng.
* **REQ-MGT-06:** Hệ thống PHẢI cung cấp một "Planning Dashboard" với chế độ xem "Gantt View" (tham chiếu `UC-MGT-02`).
* **REQ-MGT-07:** "Gantt View" PHẢI tự động đọc tất cả các đối tượng `@TSK` và hiển thị chúng trên một biểu đồ timeline dựa trên "Due Date".
* **REQ-MGT-08:** BA PHẢI có khả năng gán một Đối tượng Yêu cầu (ví dụ: `@UC001`) vào một "Release" (ví dụ: `@REL001`) thông qua một trường (field) "Target Release" trong Form (tham chiếu `UC-MGT-03`).

## 3.6 Publication (UC-PKG-PUB)
* **REQ-PUB-01:** Hệ thống PHẢI cung cấp chức năng "Export to Document (PDF/DOCX)" (tham chiếu `UC-PUB-01`).
* **REQ-PUB-02:** Chức năng Export PHẢI cho phép BA chọn một "Template Xuất bản" (đã định nghĩa trong `UC-CFG-03`).
* **REQ-PUB-03:** Chức năng Export PHẢI cho phép BA lọc (filter) các yêu cầu sẽ được xuất bản, dựa trên "Release" (đã gán trong `UC-MGT-03`).
* **REQ-PUB-04:** Hệ thống PHẢI gom (aggregate) nội dung, tạo mục lục, và gọi công cụ (Pandoc) để tạo file tài liệu cuối cùng.
* **REQ-PUB-05:** Hệ thống PHẢI cung cấp chức năng "Export to Excel" (tham chiếu `UC-PUB-02`).
* **REQ-PUB-06:** Chức năng "Export to Excel" PHẢI tạo ra một file `.xlsx` trong đó mỗi "Loại Đối tượng" (<code>@UC</code>, <code>@BR</code>...) là một "Sheet" riêng biệt.
* **REQ-PUB-07:** Dữ liệu trong các Sheet (Excel) PHẢI tương thích để có thể được tái nhập (re-import) bằng `UC-PM-03`.

---

# 4. External Interface Requirements

## 4.1 User Interfaces
* **UI-01:** Giao diện PHẢI tuân theo bố cục 3 cột (IntelliJ-like): Cột Trái (Project TreeView), Cột Giữa (Main Work Area - TabPane), Cột Phải (Contextual Info - Backlinks, Details).
* **UI-02:** Giao diện PHẢI hỗ trợ đa cửa sổ (Multi-window), cho phép BA kéo một Tab ra thành một cửa sổ (Stage) riêng biệt.
* **UI-03:** Giao diện v1.0 PHẢI có một theme "Dark Mode" (tối màu) duy nhất.
* **UI-04:** File CSS của theme PHẢI được lưu bên ngoài file build (JAR) để cho phép người dùng nâng cao tùy chỉnh (NFR).

## 4.2 Hardware Interfaces
* Không có yêu cầu giao diện phần cứng đặc biệt. Hệ thống sử dụng bàn phím, chuột và màn hình tiêu chuẩn.

## 4.3 Software Interfaces
* **SI-01 (File System):** Hệ thống PHẢI sử dụng thư viện Java (ví dụ: `java.nio.file`) để đọc/ghi trực tiếp vào hệ thống file của người dùng.
* **SI-02 (Diagramming):** Hệ thống PHẢI tích hợp thư viện **PlantUML** (dạng `.jar`) để render code sơ đồ thành `BufferedImage`.
* **SI-03 (Document Conversion):** Hệ thống PHẢI gọi (execute command) công cụ **Pandoc** được cài đặt bên ngoài để thực hiện `UC-PUB-01`.
* **SI-04 (Excel Handling):** Hệ thống PHẢI tích hợp thư viện Java (ví dụ: Apache POI) để đọc (<code>UC-PM-03</code>) và ghi (<code>UC-PUB-02</code>) file `.xlsx`.
* **SI-05 (Data Serialization):** Hệ thống PHẢI tích hợp thư viện Java (ví dụ: Jackson hoặc Gson) để serialize/deserialize dữ liệu Form ra/vào file `.json`.

## 4.4 Communications Interfaces
* **CI-01 (HTTPS):** Hệ thống PHẢI sử dụng giao thức HTTPS (qua `java.net.http.HttpClient`) để giao tiếp với các API của bên thứ ba (Gemini, Figma).

---

# 5. Nonfunctional Requirements

## 5.1 Performance Requirements
* **PERF-01 (Auto-save):** Cơ chế auto-save PHẢI hoàn tất trong vòng 2 giây sau khi người dùng dừng nhập liệu và PHẢI chạy ngầm (asynchronously) mà không làm "đóng băng" (freeze) UI.
* **PERF-02 (Form Rendering):** "Form View" (từ `UC-DEV-01`) PHẢI được render (hiển thị) trong vòng 500ms sau khi người dùng click.
* **PERF-03 (Diagram Rendering):** "Diagram View" (từ `UC-MOD-01`) PHẢI được render trong vòng 1 giây (đối với các luồng < 50 bước).
* **PERF-04 (Autocomplete):** Popup autocomplete (`@ID`) PHẢI hiển thị kết quả gần như tức thì (< 300ms) khi gõ.

## 5.2 Security Requirements
* **SEC-01 (API Keys):** Các API Key (Gemini, Figma) PHẢI được lưu trữ an toàn trong kho lưu trữ credentials của hệ điều hành hoặc file cấu hình cá nhân của người dùng, KHÔNG được lưu trong thư mục dự án (để tránh commit lên Git).

## 5.3 Software Quality Attributes
* **QA-01 (Maintainability - Khả năng bảo trì):** Code PHẢI tuân thủ chặt chẽ MVVM và SOLID. Phải sử dụng Dependency Injection (ví dụ: Guice, Spring) để quản lý các services.
* **QA-02 (Usability - Tính dễ dùng):** Toàn bộ hệ thống PHẢI hoạt động theo triết lý "No-code". BA không bao giờ được yêu cầu chỉnh sửa file `.json` hay viết code PlantUML bằng tay.
* **QA-03 (Reliability - Độ tin cậy):** Cơ chế auto-save PHẢI được bật mặc định để ngăn ngừa mất dữ liệu.
* **QA-04 (Portability - Tính đa nền tảng):** Hệ thống PHẢI chạy nhất quán trên các hệ điều hành (Windows, macOS, Linux) được hỗ trợ.

---

# 6. Other Requirements

* **OTH-01 (Data Synchronization):** Hệ thống sẽ không chủ động đồng bộ file. Trách nhiệm đồng bộ (sync) file (ví dụ: qua Git, Dropbox) thuộc về người dùng. Hệ thống PHẢI có khả năng phát hiện các thay đổi file từ bên ngoài và tự động làm mới (refresh) `TreeView` và các Tab đang mở.
* **OTH-02 (Data Duality - Nguồn dữ liệu kép):**
    * **"Source of Truth":** File `.json` (hoặc `.yaml`) lưu trữ dữ liệu cấu trúc từ Form. Đây là file mà ứng dụng đọc/ghi.
    * **"Git-Friendly Mirror":** File `.md` được auto-generate từ file `.json` trên mỗi lần lưu. File này chỉ nhằm mục đích cho Git (và con người) có thể đọc "diff" thay đổi một cách dễ dàng. BA không bao giờ được chỉnh sửa file `.md` này trực tiếp.

---

# Appendix A: Glossary

| Thuật ngữ | Định nghĩa |
| :--- | :--- |
| **RMS** | Requirements Management System. Tên của ứng dụng này. |
| **Artifact (Đối tượng Yêu cầu)** | Một đơn vị thông tin yêu cầu (ví dụ: một Use Case <code>@UC001</code>, một Business Rule <code>@BR005</code>). |
| **Artifact Type (Loại Đối tượng)** | Một "class" hoặc "template" của artifact (ví dụ: <code>@UC</code>, <code>@BR</code>), được định nghĩa bởi BA. |
| **Form Builder** | (<code>UC-CFG-01</code>) Giao diện GUI cho phép BA định nghĩa các trường (fields) cho một "Loại Đối tượng". |
| **Flow Builder** | (Một phần của <code>UC-DEV-01</code>) Một thành phần GUI đặc biệt trong "Form View" cho phép BA định nghĩa các bước và logic quy trình bằng cách kéo-thả, không cần code. |
| **Form View** | Chế độ xem mặc định của một artifact, được sinh tự động từ Form Builder, dùng để **Chỉnh sửa** (Edit). |
| **Diagram View** | Chế độ xem thứ hai của một artifact, tự động sinh sơ đồ PlantUML từ dữ liệu Form, dùng để **Xem** (Read-only). |
| **@ID** | Cú pháp liên kết (linking) chuẩn trong hệ thống (ví dụ: <code>@UC001</code>). |
| **Backlinks (Liên kết ngược)** | Chức năng cho thấy tất cả các artifact khác đang liên kết ĐẾN artifact hiện tại. |
| **Release** | (<code>UC-CFG-02</code>) Một phiên bản (version) hoặc mốc (baseline) của sản phẩm, dùng để quản lý phạm vi (scope). |
| **Export Template** | (<code>UC-CFG-03</code>) Một cấu hình do BA định nghĩa (qua GUI) quy định cấu trúc của file PDF/DOCX được xuất ra. |

# Appendix B: Analysis Models (Use Cases)
*Toàn bộ các Use Case chi tiết (đã được tạo ở các bước trước) sẽ được đưa vào đây, hoặc tham chiếu đến thư mục `UseCases/`.*

* [UseCases/Overview.md](UseCases/Overview.md)
* [UseCases/UC-PM-01.md](UseCases/UC-PM-01.md)
* ... (và tất cả các file UC khác) ...