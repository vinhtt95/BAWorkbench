#!/bin/sh
#./mvnw clean package

# Lấy đường dẫn tuyệt đối của thư mục chứa script này
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

# Chuyển đến thư mục đó (thư mục gốc của dự án)
cd "$SCRIPT_DIR"

echo "Vị trí dự án: $SCRIPT_DIR"

# Cấp quyền thực thi cho mvnw (nếu chưa có)
if [ ! -x "mvnw" ]; then
  echo "Đang cấp quyền thực thi cho mvnw..."
  chmod +x mvnw
  if [ $? -ne 0 ]; then
    echo "LỖI: Không thể cấp quyền cho mvnw. Vui lòng thử bằng tay."
    exit 1
  fi
fi

# Chạy ứng dụng bằng Maven Wrapper và goal javafx:run
./mvnw javafx:run

echo "Ứng dụng đã đóng."