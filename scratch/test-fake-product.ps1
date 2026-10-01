$loginRes = curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "@scratch/test-login.json" | ConvertFrom-Json
$token = $loginRes.data.accessToken

# Thử tạo kho cho sản phẩm productId = 99999 (không tồn tại)
$fakeInv = @{ productId = 99999; totalStock = 50 } | ConvertTo-Json
Set-Content -Path "scratch/temp-fake.json" -Value $fakeInv -Encoding UTF8

$res = curl.exe -s -X POST http://localhost:8080/api/v1/inventory -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "@scratch/temp-fake.json"
Write-Host "Ket qua voi san pham productId=99999:" $res
