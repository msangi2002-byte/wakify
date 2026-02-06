
$baseUrl = "http://localhost:8080/api/v1"
$orderId = "HP1770379631662"
$phone = "0678165524"
$password = "password123"

# Login to get token
Write-Host "Logging In..."
$loginBody = @{
    emailOrPhone = $phone
    password = $password
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResponse.data.accessToken

# Check Status
Write-Host "`nChecking Payment Status for Order: $orderId"
$headers = @{
    Authorization = "Bearer $token"
}

try {
    $statusResponse = Invoke-RestMethod -Uri "$baseUrl/payments/status/$orderId" -Method Get -Headers $headers
    Write-Host "Status Response: $($statusResponse | ConvertTo-Json -Depth 5)"
} catch {
    Write-Host "Error checking status:"
    Write-Host $_.Exception.Message
}
