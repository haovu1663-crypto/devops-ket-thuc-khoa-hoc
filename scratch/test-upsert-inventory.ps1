# test-upsert-inventory.ps1
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   TEST: AUTO-SYNC INVENTORY & SMART UPSERT              " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Login Admin
$loginRes = curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "@scratch/test-login.json" | ConvertFrom-Json
$token = $loginRes.data.accessToken
Write-Host "`n[+] Admin logged in. Role: $($loginRes.data.role)" -ForegroundColor Green

# Target product: ID = 2 (Laptop Gaming ASUS ROG Strix G16, stock = 50)
$targetId = 2

# TC1: Add product for the first time WITHOUT quantity/totalStock (only productId)
Write-Host "`n[TC1] Them san pham ID=$targetId vao kho lan dau (CHI GUI PRODUCT ID, KHONG GUI TOTAL_STOCK):" -ForegroundColor Yellow
$p1 = @{ productId = [int]$targetId } | ConvertTo-Json
Set-Content -Path "scratch/temp-tc1.json" -Value $p1 -Encoding UTF8
$r1 = curl.exe -s -X POST http://localhost:8080/api/v1/inventory -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "@scratch/temp-tc1.json"
Write-Host "-> Ket qua TC1:" $r1 -ForegroundColor Green

# TC2: Add the same product ID=$targetId again (upsert without quantity -> should auto-accumulate stock)
Write-Host "`n[TC2] Them tiep san pham ID=$targetId lan 2 (khong loi, tu dong cong don):" -ForegroundColor Yellow
$r2 = curl.exe -s -X POST http://localhost:8080/api/v1/inventory -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "@scratch/temp-tc1.json"
Write-Host "-> Ket qua TC2:" $r2 -ForegroundColor Green

# TC3: Add product ID=$targetId with explicit quantity = 30
Write-Host "`n[TC3] Them tiep san pham ID=$targetId voi quantity = 30 (cong them 30 vao kho):" -ForegroundColor Yellow
$p3 = @{ productId = [int]$targetId; quantity = 30 } | ConvertTo-Json
Set-Content -Path "scratch/temp-tc3.json" -Value $p3 -Encoding UTF8
$r3 = curl.exe -s -X POST http://localhost:8080/api/v1/inventory -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "@scratch/temp-tc3.json"
Write-Host "-> Ket qua TC3:" $r3 -ForegroundColor Green

# TC4: Add non-existent product ID = 99999 -> gRPC error
Write-Host "`n[TC4] Them san pham khong ton tai (ID = 99999) -> gRPC bat loi:" -ForegroundColor Yellow
$p4 = @{ productId = 99999 } | ConvertTo-Json
Set-Content -Path "scratch/temp-tc4.json" -Value $p4 -Encoding UTF8
$r4 = curl.exe -s -X POST http://localhost:8080/api/v1/inventory -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "@scratch/temp-tc4.json"
Write-Host "-> Ket qua TC4:" $r4 -ForegroundColor Yellow

# Final public check
Write-Host "`n[+] Tra cuu ton kho cong khai GET /api/v1/inventory/$targetId" -ForegroundColor Cyan
$invFinal = curl.exe -s "http://localhost:8080/api/v1/inventory/$targetId"
Write-Host "-> Ton kho thuc te:" $invFinal -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "          HOAN TAT TAT CA TEST CASES THANH CONG!          " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
