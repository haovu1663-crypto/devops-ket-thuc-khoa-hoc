-- =======================================================
-- Khởi tạo các Database độc lập cho từng Microservice
-- Theo chuẩn Database Per Service Pattern
-- =======================================================

-- 1. Database cho Identity & User Service (Quản lý User, Role, Refresh Token)
SELECT 'CREATE DATABASE identity_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'identity_db')\gexec

-- 2. Database cho Product Catalog Service (Quản lý Sản phẩm)
SELECT 'CREATE DATABASE product_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'product_db')\gexec

-- 3. Database cho Order & Inventory Service (Quản lý Đơn hàng, Tồn kho)
SELECT 'CREATE DATABASE order_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'order_db')\gexec

-- 4. Database cho Payment & Billing Service (Quản lý Giao dịch, Cổng thanh toán)
SELECT 'CREATE DATABASE payment_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')\gexec

-- 5. Database cho Notification Service (Lịch sử gửi email)
SELECT 'CREATE DATABASE notification_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec

-- Gán quyền cho user postgres
GRANT ALL PRIVILEGES ON DATABASE identity_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE product_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
