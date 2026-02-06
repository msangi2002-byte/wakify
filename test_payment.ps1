
$baseUrl = "http://localhost:8080/api/v1"
$phone = "0678165524"
$password = "password123"

# 1. Register
Write-Host "Registering User..."
$registerBody = @{
    name = "Test User"
    phone = $phone
    password = $password
    role = "USER"
} | ConvertTo-Json

try {
    $regResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $registerBody -ContentType "application/json"
    Write-Host "Registration Response: $($regResponse | ConvertTo-Json -Depth 5)"
} catch {
    Write-Host "Registration failed or user already exists (continuing to login...)"
    Write-Host $_.Exception.Message
}

# 2. Login
Write-Host "`nLogging In..."
$loginBody = @{
    emailOrPhone = $phone
    password = $password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    
    if ($loginResponse.success -eq $true) {
        $token = $loginResponse.data.accessToken
        Write-Host "Login Successful! Token received."

        # 3. Initiate Payment
        Write-Host "`nInitiating HarakaPay Payment..."
        $paymentBody = @{
            amount = 1000
            type = "ORDER"
            phone = $phone
            description = "Test HarakaPay Payment for $phone"
        } | ConvertTo-Json

        $headers = @{
            Authorization = "Bearer $token"
        }

        $paymentResponse = Invoke-RestMethod -Uri "$baseUrl/payments/initiate" -Method Post -Body $paymentBody -ContentType "application/json" -Headers $headers
        Write-Host "Payment Response: $($paymentResponse | ConvertTo-Json -Depth 5)"
    } else {
        Write-Host "Login Failed!"
    }
} catch {
    Write-Host "Error occurred:"
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody"
    }
}
