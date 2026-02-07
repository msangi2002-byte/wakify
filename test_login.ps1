# Test Login API - Wakilfly
# Usage: .\test_login.ps1 [baseUrl]
# Default baseUrl: http://localhost:8080

param(
    [string]$baseUrl = "http://localhost:8080"
)

$loginUrl = "$baseUrl/api/v1/auth/login"

Write-Host "=== Wakilfly Login API Test ===" -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl"
Write-Host "POST $loginUrl"
Write-Host ""

# Test 1: Login with phone (test user from DataInitializer)
Write-Host "1. Login with PHONE (255712000000 / test123)..." -ForegroundColor Yellow
$bodyPhone = @{ emailOrPhone = "255712000000"; password = "test123" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $bodyPhone -ContentType "application/json" -TimeoutSec 10
    if ($r.success -and $r.data.accessToken) {
        Write-Host "   OK - Got accessToken" -ForegroundColor Green
        Write-Host "   User: $($r.data.user.name), role: $($r.data.user.role)"
    } else {
        Write-Host "   FAIL - Unexpected response: $($r | ConvertTo-Json -Depth 3)" -ForegroundColor Red
    }
} catch {
    Write-Host "   FAIL - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) { Write-Host "   Body: $($_.ErrorDetails.Message)" }
}
Write-Host ""

# Test 2: Login with email
Write-Host "2. Login with EMAIL (test@wakilfy.com / test123)..." -ForegroundColor Yellow
$bodyEmail = @{ emailOrPhone = "test@wakilfy.com"; password = "test123" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $bodyEmail -ContentType "application/json"
    if ($r.success -and $r.data.accessToken) {
        Write-Host "   OK - Got accessToken" -ForegroundColor Green
        Write-Host "   User: $($r.data.user.name)"
    } else {
        Write-Host "   FAIL - Unexpected response" -ForegroundColor Red
    }
} catch {
    Write-Host "   FAIL - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) { Write-Host "   Body: $($_.ErrorDetails.Message)" }
}
Write-Host ""

# Test 3: Wrong password
Write-Host "3. Wrong password (expect 401)..." -ForegroundColor Yellow
$bodyWrong = @{ emailOrPhone = "255712000000"; password = "wrong" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $bodyWrong -ContentType "application/json"
    Write-Host "   FAIL - Should have returned error" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "   OK - Got 401 Unauthorized as expected" -ForegroundColor Green
    } else {
        Write-Host "   Got: $($_.Exception.Message)" -ForegroundColor Red
    }
}
Write-Host ""

# Test 4: Missing user
Write-Host "4. Unknown user (expect 401)..." -ForegroundColor Yellow
$bodyUnknown = @{ emailOrPhone = "255999999999"; password = "test123" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $bodyUnknown -ContentType "application/json"
    Write-Host "   FAIL - Should have returned error" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "   OK - Got 401 as expected" -ForegroundColor Green
    } else {
        Write-Host "   Got: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Cyan
