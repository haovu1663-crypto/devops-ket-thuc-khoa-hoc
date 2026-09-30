# API Gateway Service (Gradle)

Dịch vụ Cổng vào duy nhất (**Single Point of Entry**) cho Hệ thống Thương mại Điện tử Đa kênh chuẩn Doanh nghiệp.

---

## 1. Công nghệ sử dụng
* **Java 21 / 22**
* **Build Tool: Gradle 8.10.2** (kèm Gradle Wrapper `gradlew`)
* **Spring Boot 3.3.4**
* **Spring Cloud Gateway (Reactive / Netty)**
* **JJWT (Java JSON Web Token 0.12.6)**
* **Spring Boot Actuator** (Health check & Metrics)
* **Docker Multi-Stage Build** (Eclipse Temurin JRE 21 Alpine)

---

## 2. Tính năng chính
1. **Dynamic Routing:** Điều hướng request từ Client (Web/Mobile) đến 5 Microservices phía sau.
2. **Centralized Authentication (Stateless JWT):**
   * Tự động kiểm tra header `Authorization: Bearer <token>` trên các route bảo mật.
   * Giải mã và xác thực chữ ký số JWT trực tiếp trên RAM.
   * Chặn ngay lập tức request không hợp lệ (`401 Unauthorized`).
   * Tự động inject thông tin danh tính người dùng vào Header (`X-User-Id`, `X-User-Username`, `X-User-Role`) để các Microservice nội bộ tái sử dụng mà không cần query lại database.
3. **CORS Support:** Cấu hình mở rộng cho các client Web SPA và Mobile Apps kết nối an toàn.
4. **Structured Logging:** Ghi log tập trung mọi request đi vào và phản hồi đi ra (Path, Method, Client IP, Latency, Status Code).
5. **Observability:** Sẵn sàng cho Prometheus & Grafana scrape metrics tại `/actuator/metrics`.

---

## 3. Bảng định tuyến (Routing Table)

| STT | Route ID | Đường dẫn (Path) | Microservice đích | Yêu cầu JWT? |
| :---: | :--- | :--- | :--- | :---: |
| 1 | `identity-service-auth` | `/api/v1/auth/**` | `http://identity-service:8081` | ❌ (Public) |
| 2 | `identity-service-users` | `/api/v1/users/**` | `http://identity-service:8081` | ✅ (Cần Token) |
| 3 | `product-service-public` | `/api/v1/products/**`, `/api/v1/categories/**` (GET) | `http://product-service:8082` | ❌ (Public) |
| 4 | `product-service-admin` | `/api/v1/products/**` (POST, PUT, DELETE) | `http://product-service:8082` | ✅ (Cần Token) |
| 5 | `order-service` | `/api/v1/orders/**`, `/api/v1/inventory/**` | `http://order-service:8083` | ✅ (Cần Token) |
| 6 | `payment-service-webhook` | `/api/v1/payments/webhook/**` | `http://payment-service:8084` | ❌ (Public) |
| 7 | `payment-service-secured` | `/api/v1/payments/**` | `http://payment-service:8084` | ✅ (Cần Token) |
| 8 | `notification-service` | `/api/v1/notifications/**` | `http://notification-service:8085` | ✅ (Cần Token) |

---

## 4. Hướng dẫn chạy thử nghiệm với Gradle

### Cách 1: Chạy trực tiếp qua Gradle Wrapper
```bash
# Di chuyển vào thư mục api-gateway
cd api-gateway

# Chạy ứng dụng trên Windows PowerShell
.\gradlew.bat bootRun

# Hoặc trên Linux/macOS:
./gradlew bootRun
```

### Cách 2: Đóng gói JAR
```bash
# Build file jar (sẽ tạo trong build/libs/)
.\gradlew.bat bootJar -x test

# Chạy file jar vừa build
java -jar build/libs/api-gateway-1.0.0-SNAPSHOT.jar
```

### Cách 3: Đóng gói và chạy bằng Docker
```bash
# Build Docker image
docker build -t ecommerce-api-gateway:latest .

# Chạy container
docker run -d -p 8080:8080 --name api-gateway ecommerce-api-gateway:latest
```

---

## 5. Kiểm tra Health Check
Sau khi khởi động, kiểm tra trạng thái hoạt động:
```bash
curl http://localhost:8080/actuator/health
```
Kết quả mong đợi:
```json
{"status":"UP"}
```
