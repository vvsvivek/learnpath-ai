$BaseUrl = "https://ai-learning-backend-ztk7.onrender.com"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  LEARNPATH AI - LIVE AUTHENTICATION WORKFLOW VERIFICATION" -ForegroundColor Cyan
Write-Host "==========================================================`n" -ForegroundColor Cyan

# -------------------------------------------------------------
# 1. STUDENT LOGIN WORKFLOW (@vrsec.ac.in)
# -------------------------------------------------------------
$studentEmail = "test.student@vrsec.ac.in"
Write-Host "[1] STUDENT AUTHENTICATION: $studentEmail" -ForegroundColor Yellow

# Step 1A: Request OTP
Write-Host "  -> Requesting OTP via POST /api/auth/send-otp..."
$studentSendRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/send-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{ email = $studentEmail } | ConvertTo-Json)
Write-Host "     Success: $($studentSendRes.success)"
Write-Host "     Message: $($studentSendRes.message)"
Write-Host "     Brevo Dispatched: $($studentSendRes.dispatched)"

# Step 1B: Verify OTP & Complete Login
Write-Host "  -> Verifying OTP & logging in via POST /api/auth/verify-otp..."
$studentVerifyBody = @{
    email = $studentEmail
    otp = "123456"
    username = "Test Student VRSEC"
} | ConvertTo-Json

$studentAuthRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/verify-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body $studentVerifyBody
$studentToken = $studentAuthRes.token
$studentRole = $studentAuthRes.role
$studentUser = $studentAuthRes.user

Write-Host "     JWT Token Generated: $(if ($studentToken) { 'YES (Length: ' + $studentToken.Length + ')' } else { 'NO' })"
Write-Host "     Role Assigned: $studentRole (Expected: STUDENT)"
Write-Host "     User ID: $($studentUser.id), Username: $($studentUser.username)"
Write-Host "     Branch: $($studentUser.branch), Year: $($studentUser.academicYear)"

# Step 1C: Test Authenticated Call with Student JWT
Write-Host "  -> Testing Authenticated Call (GET /api/main/dashboard) with Student JWT..."
$studentDash = Invoke-RestMethod -Uri "$BaseUrl/api/main/dashboard?email=$studentEmail" -Method GET -Headers @{ "Authorization" = "Bearer $studentToken" }
Write-Host "     Dashboard Access: SUCCESS (Active Courses: $($studentDash.activeCourses.Count))`n"

# -------------------------------------------------------------
# 2. FACULTY LOGIN WORKFLOW (@siddhartha.ac.in)
# -------------------------------------------------------------
$facultyEmail = "faculty.test@siddhartha.ac.in"
Write-Host "[2] FACULTY AUTHENTICATION: $facultyEmail" -ForegroundColor Yellow

# Step 2A: Request OTP
Write-Host "  -> Requesting OTP via POST /api/auth/send-otp..."
$facultySendRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/send-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{ email = $facultyEmail } | ConvertTo-Json)
Write-Host "     Success: $($facultySendRes.success)"
Write-Host "     Message: $($facultySendRes.message)"
Write-Host "     Brevo Dispatched: $($facultySendRes.dispatched)"

# Step 2B: Verify OTP & Complete Login
Write-Host "  -> Verifying OTP & logging in via POST /api/auth/verify-otp..."
$facultyVerifyBody = @{
    email = $facultyEmail
    otp = "123456"
    username = "Test Faculty Siddhartha"
} | ConvertTo-Json

$facultyAuthRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/verify-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body $facultyVerifyBody
$facultyToken = $facultyAuthRes.token
$facultyRole = $facultyAuthRes.role
$facultyUser = $facultyAuthRes.user

Write-Host "     JWT Token Generated: $(if ($facultyToken) { 'YES (Length: ' + $facultyToken.Length + ')' } else { 'NO' })"
Write-Host "     Role Assigned: $facultyRole (Expected: FACULTY)"
Write-Host "     User ID: $($facultyUser.id), Username: $($facultyUser.username)"

# Step 2C: Test Authenticated Call with Faculty JWT
Write-Host "  -> Testing Authenticated Call (GET /api/faculty/overview) with Faculty JWT..."
$facultyOverview = Invoke-RestMethod -Uri "$BaseUrl/api/faculty/overview" -Method GET -Headers @{ "Authorization" = "Bearer $facultyToken" }
Write-Host "     Faculty Analytics Access: SUCCESS (Total Eligible Students: $($facultyOverview.totalEligibleStudents))`n"

# -------------------------------------------------------------
# 3. DOMAIN SECURITY ENFORCEMENT
# -------------------------------------------------------------
Write-Host "[3] DOMAIN SECURITY ENFORCEMENT (Unauthorized Domain Rejection)" -ForegroundColor Yellow
$unauthEmail = "random.user@gmail.com"
Write-Host "  -> Attempting OTP dispatch to unauthorized email: $unauthEmail..."
try {
    $unauthRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/send-otp" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{ email = $unauthEmail } | ConvertTo-Json)
    Write-Host "     UNEXPECTED SUCCESS: Unauthorized email was accepted!" -ForegroundColor Red
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $msg = $reader.ReadToEnd()
    Write-Host "     Correctly Rejected with Status $($_.Exception.Response.StatusCode.value__): $msg" -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  AUTHENTICATION VERIFICATION COMPLETE" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
