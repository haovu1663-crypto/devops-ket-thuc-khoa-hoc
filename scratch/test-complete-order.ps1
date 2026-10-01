$loginRes = curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "@scratch/test-login.json" | ConvertFrom-Json
$token = $loginRes.data.accessToken

# 1. Chuyển sang SHIPPING
$shipBody = @{ status = "SHIPPING"; reason = "Dang giao hang" } | ConvertTo-Json
Set-Content -Path "scratch/temp-ship.json" -Value $shipBody -Encoding UTF8
$shipRes = curl.exe -s -X PATCH "http://localhost:8080/api/v1/orders/1/status" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "@scratch/temp-ship.json"
Write-Host "SHIPPING:" $shipRes

# 2. Chuyển sang COMPLETED
$compBody = @{ status = "COMPLETED"; reason = "Giao hang thanh cong" } | ConvertTo-Json
Set-Content -Path "scratch/temp-comp.json" -Value $compBody -Encoding UTF8
$compRes = curl.exe -s -X PATCH "http://localhost:8080/api/v1/orders/1/status" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "@scratch/temp-comp.json"
Write-Host "COMPLETED:" $compRes

# 3. Kiểm tra tồn kho cuối cùng
$inv = curl.exe -s "http://localhost:8080/api/v1/inventory/1"
Write-Host "INVENTORY FINAL:" $inv
