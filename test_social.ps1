
$baseUrl = "http://localhost:8080/api/v1"

# --- Helper Function: Register and Login ---
function Get-Token {
    param ($name, $phone)
    $password = "password123"

    Write-Host "Registering $name..."
    $regBody = @{ name = $name; phone = $phone; password = $password; role = "USER" } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $regBody -ContentType "application/json" | Out-Null
    }
    catch { 
        # Ignore if user exists 
    }

    Write-Host "Logging in $name..."
    $loginBody = @{ emailOrPhone = $phone; password = $password } | ConvertTo-Json
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    
    return $loginRes.data.accessToken
}

# 1. Setup Users
$token1 = Get-Token "User One" "0655555551"
$token2 = Get-Token "User Two" "0655555552"

$headers1 = @{ Authorization = "Bearer $token1" }
$headers2 = @{ Authorization = "Bearer $token2" }

# 2. User 1 Creates a Post
Write-Host "`nUser 1 Creating Post..."
# Using multipart form data is tricky in pure PowerShell with Invoke-RestMethod, 
# relying on simplified JSON post creation if controller supports it, or minimal simulation.
# Note: Our controller expects MULTIPART, so we need a boundary trick.

$boundary = [System.Guid]::NewGuid().ToString() 
$LF = "`r`n"
$bodyLines = ( 
    "--$boundary",
    "Content-Disposition: form-data; name=`"data`"",
    "Content-Type: application/json",
    "",
    '{ "caption": "Hello world! This is my first post with new features.", "visibility": "PUBLIC" }',
    "--$boundary--" 
) -join $LF

$post1 = Invoke-RestMethod -Uri "$baseUrl/posts" -Method Post -ContentType "multipart/form-data; boundary=$boundary" -Body $bodyLines -Headers $headers1

$postId = $post1.data.id
Write-Host "Post Created! ID: $postId"

# 3. User 2 Reacts to Post (LOVE)
Write-Host "`nUser 2 Reacting (LOVE) to Post..."
$reactRes = Invoke-RestMethod -Uri "$baseUrl/posts/$postId/react?type=LOVE" -Method Post -Headers $headers2
Write-Host "Reaction Response: $($reactRes | ConvertTo-Json -Depth 2)"

# 4. Verify Reaction
Write-Host "`nVerifying Reaction..."
$postDetails = Invoke-RestMethod -Uri "$baseUrl/posts/$postId" -Method Get -Headers $headers2
Write-Host "Reactions Count: $($postDetails.data.reactionsCount)"
Write-Host "My Reaction: $($postDetails.data.userReaction)"

if ($postDetails.data.userReaction -eq "LOVE") {
    Write-Host "SUCCESS: Reaction is correct!" -ForegroundColor Green
}
else {
    Write-Host "FAILURE: Reaction mismatch." -ForegroundColor Red
}

# 5. User 2 Shares (Reposts) the Post
Write-Host "`nUser 2 Reposting..."
# Repost body just needs originalPostId. Caption is optional for shared post.
$repostBodyLines = ( 
    "--$boundary",
    "Content-Disposition: form-data; name=`"data`"",
    "Content-Type: application/json",
    "",
    "{ ""caption"": ""Check this out! Amazing."", ""originalPostId"": ""$postId"" }",
    "--$boundary--" 
) -join $LF

$repostRes = Invoke-RestMethod -Uri "$baseUrl/posts" -Method Post -ContentType "multipart/form-data; boundary=$boundary" -Body $repostBodyLines -Headers $headers2
$repostId = $repostRes.data.id
Write-Host "Repost Created! ID: $repostId"

# 6. Verify Repost Details
Write-Host "`nVerifying Repost..."
$repostDetails = Invoke-RestMethod -Uri "$baseUrl/posts/$repostId" -Method Get -Headers $headers2
$orig = $repostDetails.data.originalPost

Write-Host "Repost Caption: $($repostDetails.data.caption)"
if ($orig -ne $null -and $orig.id -eq $postId) {
    Write-Host "SUCCESS: Repost correctly linked to Original Post ($($orig.id))" -ForegroundColor Green
}
else {
    Write-Host "FAILURE: Original Post not linked." -ForegroundColor Red
}
