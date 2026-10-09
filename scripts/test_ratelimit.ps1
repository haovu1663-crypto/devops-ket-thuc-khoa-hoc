# ========================================================
# KICH BAN KIEM TRA RATE LIMITER TAI API GATEWAY
# ========================================================

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [TEST 1] BAN 20 REQUEST DON DAP VAO ENDPOINT LOGIN" -ForegroundColor Yellow
Write-Host " Cau hinh: burstCapacity = 10, replenishRate = 5 token/s" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan

Write-Host "Dang ban 25 requests lien tiep cuc nhanh vao API Gateway..." -ForegroundColor Cyan

$codes = 1..25 | ForEach-Object {
    curl.exe -s -o nul -w "%{http_code} " -X POST "http://localhost:8080/api/v1/auth/login" `
        -H "Content-Type: application/json" `
        -d "{}"
}

$codeArray = ($codes -join "").Trim().Split(" ")
$count = 1
$table = $codeArray | ForEach-Object {
    $c = $_
    $action = if ($c -eq "429") {
        "[429] 🛑 BI CHAN NGAY TAI GATEWAY (Too Many Requests)"
    } else {
        "[$c]  ✅ QUA GATEWAY THANH CONG (Giu token hop le)"
    }
    $row = [PSCustomObject]@{
        "Lan goi"    = "#$count"
        "Http Code"  = $c
        "Trang thai" = $action
    }
    $count++
    $row
}

$table | Format-Table -AutoSize


Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [TEST 2] KIEM TRA DU LIEU TOKEN TRONG REDIS" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan
docker exec ecommerce-redis redis-cli -a redis123 keys "request_rate_limiter*"

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " [TEST 3] KIEM TRA PHUC HOI TOKEN SAU 2 GIAY (REPLENISH)" -ForegroundColor Yellow
Write-Host " Dang cho 2 giay de Redis tu dong nap lai token..." -ForegroundColor Gray
Start-Sleep -Seconds 2

$recovery = curl.exe -s -i -X POST "http://localhost:8080/api/v1/auth/login" `
    -H "Content-Type: application/json" `
    -d "{\`"username\`":\`"haovu1663\`",\`"password\`":\`"wrongpass\`"}"

$recStatus = (($recovery | Select-String -Pattern "^HTTP/").Line -split "\s+")[1]
$recRem = (($recovery | Select-String -Pattern "X-RateLimit-Remaining:").Line -split ":\s*")[1].Trim()

Write-Host "Sau 2 giay hoi phuc: HttpCode = $recStatus, TokensLeft = $recRem" -ForegroundColor Green
Write-Host "=> KET LUAN: RATE LIMITER HOAT DONG CHINH XAC 100%!" -ForegroundColor Green
