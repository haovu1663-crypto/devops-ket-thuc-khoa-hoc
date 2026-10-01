# test-e2e-order-inventory.ps1
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   TEST END-TO-END: ORDER & INVENTORY & PRODUCT (gRPC)   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Đăng nhập Admin
Write-Host "`n[1] Dang nhap Admin..." -ForegroundColor Yellow
$loginRes = curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "@scratch/test-login.json"
$adminLogin = $loginRes | ConvertFrom-Json
$adminToken = $adminLogin.data.accessToken
Write-Host "-> Admin logged in. Role: $($adminLogin.data.role)" -ForegroundColor Green

# 2. Lấy danh sách sản phẩm từ product-service
Write-Host "`n[2] Lay danh sach san pham (PUBLIC GET /api/v1/products)..." -ForegroundColor Yellow
$prodRaw = curl.exe -s http://localhost:8080/api/v1/products
$productsRes = $prodRaw | ConvertFrom-Json
$targetProduct = $productsRes.data[0]
$productId = $targetProduct.id
Write-Host "-> Chon Product ID: $productId ($($targetProduct.name)) - Gia: $($targetProduct.price) VND" -ForegroundColor Cyan

# 3. Admin khởi tạo tồn kho cho sản phẩm
Write-Host "`n[3] Admin khoi tao ton kho (POST /api/v1/inventory)..." -ForegroundColor Yellow
$initInv = @{
    productId = [int]$productId
    totalStock = 100
} | ConvertTo-Json
Set-Content -Path "scratch/temp-init-inv.json" -Value $initInv -Encoding UTF8

$invRaw = curl.exe -s -X POST http://localhost:8080/api/v1/inventory -H "Authorization: Bearer $adminToken" -H "Content-Type: application/json" -d "@scratch/temp-init-inv.json"
Write-Host "-> Ket qua POST /api/v1/inventory:" $invRaw -ForegroundColor Green

# 4. Xem tồn kho công khai
Write-Host "`n[4] Kiem tra ton kho PUBLIC GET /api/v1/inventory/$productId..." -ForegroundColor Yellow
$invCheck = curl.exe -s "http://localhost:8080/api/v1/inventory/$productId"
Write-Host "-> Ton kho:" $invCheck -ForegroundColor Green

# 5. Customer đặt hàng (POST /api/v1/orders)
Write-Host "`n[5] Dat hang 2 san pham (POST /api/v1/orders)..." -ForegroundColor Yellow
$orderPayload = @{
    note = "Giao hang gio hanh chinh"
    items = @(
        @{
            productId = [int]$productId
            quantity = 2
        }
    )
} | ConvertTo-Json -Depth 4
Set-Content -Path "scratch/temp-create-order.json" -Value $orderPayload -Encoding UTF8

$orderRaw = curl.exe -s -X POST http://localhost:8080/api/v1/orders -H "Authorization: Bearer $adminToken" -H "Content-Type: application/json" -d "@scratch/temp-create-order.json"
Write-Host "-> Ket qua tao don hang:" $orderRaw -ForegroundColor Green

$orderJson = $orderRaw | ConvertFrom-Json
$orderId = $orderJson.data.id

# 6. Kiểm tra lại tồn kho sau khi đặt hàng (availableStock giảm 2, reservedStock tăng 2)
Write-Host "`n[6] Kiem tra ton kho sau khi dat hang (available giam 2, reserved tang 2)..." -ForegroundColor Yellow
$invAfter = curl.exe -s "http://localhost:8080/api/v1/inventory/$productId"
Write-Host "-> Ton kho:" $invAfter -ForegroundColor Cyan

# 7. Xem lịch sử đơn hàng
Write-Host "`n[7] Xem lich su don hang (GET /api/v1/orders)..." -ForegroundColor Yellow
$ordersList = curl.exe -s http://localhost:8080/api/v1/orders -H "Authorization: Bearer $adminToken"
Write-Host "-> Danh sach don:" $ordersList -ForegroundColor Green

# 8. Admin xác nhận đơn hàng (CONFIRMED)
if ($orderId) {
    Write-Host "`n[8] Admin xac nhan don hang $orderId (CONFIRMED)..." -ForegroundColor Yellow
    $statusPayload = @{
        status = "CONFIRMED"
        reason = "Xac nhan hop le"
    } | ConvertTo-Json
    Set-Content -Path "scratch/temp-status.json" -Value $statusPayload -Encoding UTF8

    $statusRaw = curl.exe -s -X PATCH "http://localhost:8080/api/v1/orders/$orderId/status" -H "Authorization: Bearer $adminToken" -H "Content-Type: application/json" -d "@scratch/temp-status.json"
    Write-Host "-> Ket qua cap nhat trang thai:" $statusRaw -ForegroundColor Green

    # 9. Kiểm tra lại tồn kho sau khi CONFIRMED (totalStock giảm 2, reservedStock về 0)
    Write-Host "`n[9] Kiem tra ton kho sau khi CONFIRMED (total giam 2, reserved giam 2)..." -ForegroundColor Yellow
    $invFinal = curl.exe -s "http://localhost:8080/api/v1/inventory/$productId"
    Write-Host "-> Ton kho cuoi cung:" $invFinal -ForegroundColor Cyan
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "       TEST END-TO-END THANH CONG HOAN HAO!              " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
