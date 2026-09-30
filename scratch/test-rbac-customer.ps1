$loginRes = curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "@scratch/test-login.json" | ConvertFrom-Json
$token = $loginRes.data.accessToken

Write-Host "Token Role: " $loginRes.data.role
Write-Host "Calling POST /api/v1/products with ROLE_CUSTOMER token..."

$res = curl.exe -s -X POST http://localhost:8080/api/v1/products -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "@scratch/test-product.json"
Write-Host "Response:" $res
