# Use Case Overview

## 1. Use Case Diagram

(Đây là sơ đồ PlantUML cấp cao mô tả các gói chức năng chính)

```plantuml
@startuml
left to right direction
actor "Business Analyst (BA)" as BA

rectangle "Hệ thống Quản lý Yêu cầu (RMS)" {
  (UC-PKG-PM: Quản lý Dự án)
  (UC-PKG-CFG: Cấu hình Dự án)
  (UC-PKG-DEV: Phát triển Yêu cầu)
  (UC-PKG-MOD: Mô hình hóa Yêu cầu)
  (UC-PKG-MGT: Quản lý Yêu cầu)
  (UC-PKG-PUB: Xuất bản Tài liệu)
}

BA -- UC-PKG-PM
BA -- UC-PKG-CFG
BA -- UC-PKG-DEV
BA -- UC-PKG-MOD
BA -- UC-PKG-MGT
BA -- UC-PKG-PUB
@enduml