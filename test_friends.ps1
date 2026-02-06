
$baseUrl = "http://localhost:8080/api/v1"

# --- Helper Function: Get-Token ---
function Get-Token {
    param ($phone)
    $password = "password123"
    $loginBody = @{ emailOrPhone = $phone; password = $password } | ConvertTo-Json
    try {
        $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
        return $loginRes
    }
    catch {
        return $null
    }
}

# Assumes users created in previous tests (0655555551, 0655555552)
# If not, run test_social.ps1 first or register them here.

Write-Host "Logging in Users..."
$u1 = Get-Token "0655555551"
$u2 = Get-Token "0655555552"

if ($u1 -eq $null) { Write-Error "User 1 login failed. Run test_social.ps1 first?"; exit }
if ($u2 -eq $null) { Write-Error "User 2 login failed."; exit }

$token1 = $u1.data.accessToken
$token2 = $u2.data.accessToken
$id1 = $u1.data.user.id
$id2 = $u2.data.user.id

$h1 = @{ Authorization = "Bearer $token1" }
$h2 = @{ Authorization = "Bearer $token2" }

Write-Host "User 1 ID: $id1"
Write-Host "User 2 ID: $id2"

# 1. User 1 sends Friend Request to User 2
Write-Host "`n1. User 1 sending Friend Request..."
try {
    $reqRes = Invoke-RestMethod -Uri "$baseUrl/friends/request/$id2" -Method Post -Headers $h1
    Write-Host "Request Sent: $($reqRes.message)" -ForegroundColor Green
}
catch {
    Write-Host "Error sending request: $_" -ForegroundColor Red
}

# 2. User 2 checks Pending Requests
Write-Host "`n2. User 2 checking pending requests..."
$pendingRes = Invoke-RestMethod -Uri "$baseUrl/friends/requests" -Method Get -Headers $h2
Write-Host "Raw Response: $($pendingRes | ConvertTo-Json -Depth 5)"
$requests = $pendingRes.data.content
Write-Host "Pending Requests Count: $($requests.Count)"

$friendshipId = $null
if ($requests.Count -gt 0) {
    # Find request from User 1
    $myReq = $requests | Where-Object { $_.requester.id -eq $id1 }
    if ($myReq) {
        $friendshipId = $myReq.id
        Write-Host "Found Request from User 1. Friendship ID: $friendshipId" -ForegroundColor Green
    }
    else {
        Write-Host "Request from User 1 not found in list." -ForegroundColor Yellow
    }
}
else {
    Write-Host "No pending requests found." -ForegroundColor Yellow
}

# 3. User 2 Accepts Request
if ($friendshipId) {
    Write-Host "`n3. User 2 Accepting Request..."
    try {
        $accRes = Invoke-RestMethod -Uri "$baseUrl/friends/accept/$friendshipId" -Method Post -Headers $h2
        Write-Host "Response: $($accRes.message)" -ForegroundColor Green
    }
    catch {
        Write-Host "Error accepting: $_" -ForegroundColor Red
    }
    
    # 4. Verify Friends List
    Write-Host "`n4. Verifying Friends List for User 1..."
    $friends1 = Invoke-RestMethod -Uri "$baseUrl/friends" -Method Get -Headers $h1
    $isFriend = $friends1.data.content | Where-Object { ($_.requester.id -eq $id2) -or ($_.addressee.id -eq $id2) }
    
    if ($isFriend) {
        Write-Host "User 2 is in User 1's friend list! SUCCESS" -ForegroundColor Green
    }
    else {
        Write-Host "Friend verification failed." -ForegroundColor Red
    }

    # 5. Verify Auto-Follow (Optional if endpoint exists)
    # Check User 1 profile to see following count increased?
    # Or check if User 1 follows User 2
    Write-Host "`n5. Verifying Auto-Follow (User 1 details)..."
    $user1Profile = Invoke-RestMethod -Uri "$baseUrl/users/$id1" -Method Get -Headers $h2
    Write-Host "User 1 Followers Count: $($user1Profile.data.followersCount)"
    Write-Host "User 1 Following Count: $($user1Profile.data.followingCount)"
    
}
