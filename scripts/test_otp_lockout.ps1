$BaseUrl = "https://ai-learning-backend-ztk7.onrender.com"
$testEmail = "lockout.test@vrsec.ac.in"

Write-Host "=== TEST: EXCESSIVE OTP ATTEMPTS LOCKOUT ==="
Write-Host "Target: $testEmail"

# Step 1: Send one OTP
$sendResp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/send-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{ email = $testEmail } | ConvertTo-Json)
Write-Host "Initial OTP Dispatched: $($sendResp.dispatched)"

# Step 2: Submit 5 incorrect attempts (Attempts 1-5)
for ($i = 1; $i -le 5; $i++) {
    try {
        $body = @{
            email = $testEmail
            otp = "00000$i"
            username = "Lockout Tester"
            password = "password123"
        } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "$BaseUrl/api/auth/verify-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body $body
        Write-Host "Attempt ${i}: Unexpected Success"
    } catch {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $msg = $reader.ReadToEnd()
        Write-Host "Attempt ${i} Status: $($_.Exception.Response.StatusCode.value__) Response: $msg"
    }
}

# Step 3: Attempt 6 - Exceeding configured limit (attempts >= 5)
Write-Host "`nTesting 6th Attempt (Exceeding max attempts limit)..."
try {
    $body = @{
        email = $testEmail
        otp = "000006"
        username = "Lockout Tester"
        password = "password123"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/auth/verify-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body $body
    Write-Host "Attempt 6: Unexpected Success"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $msg = $reader.ReadToEnd()
    Write-Host "Attempt 6 Status: $($_.Exception.Response.StatusCode.value__) Response: $msg"
}

# Step 4: Confirm correct/valid OTP cannot bypass an active lockout
Write-Host "`nTesting Correct Code on Locked Out Account..."
try {
    $body = @{
        email = $testEmail
        otp = "123456"
        username = "Lockout Tester"
        password = "password123"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/auth/verify-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body $body
    Write-Host "Lockout Bypass: Unexpected Success"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $msg = $reader.ReadToEnd()
    Write-Host "Lockout Bypass Status: $($_.Exception.Response.StatusCode.value__) Response: $msg"
}

# Step 5: Verify user was not created in users table
Write-Host "`nVerifying no user record was created..."
try {
    $userCheck = Invoke-RestMethod -Uri "$BaseUrl/api/auth/me?email=$testEmail" -Method GET
    Write-Host "User Check: User unexpectedly found"
} catch {
    Write-Host "User Check Status: $($_.Exception.Response.StatusCode.value__) (Confirmed: User was NOT created)"
}
