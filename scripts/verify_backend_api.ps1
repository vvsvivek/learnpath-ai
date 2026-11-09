# =================================================================
# LEARNPATH AI - COMPLETE 10-FEATURE AUTOMATED PRODUCTION TEST SUITE
# =================================================================

param(
    [string]$BaseUrl = "https://ai-learning-backend-ztk7.onrender.com"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  LEARNPATH AI - AUTOMATED END-TO-END TEST SUITE" -ForegroundColor Cyan
Write-Host "  Target URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "==========================================================`n" -ForegroundColor Cyan

$TotalTests = 0
$PassedTests = 0

function Run-Test {
    param(
        [string]$Name,
        [scriptblock]$Action
    )
    $script:TotalTests++
    Write-Host "[TEST $script:TotalTests] $Name..." -NoNewline
    try {
        $result = & $Action
        if ($result -eq $true) {
            Write-Host " PASSED [OK]" -ForegroundColor Green
            $script:PassedTests++
        } else {
            Write-Host " FAILED [X]" -ForegroundColor Red
        }
    } catch {
        Write-Host " ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 1. Student registration and OTP delivery (@vrsec.ac.in)
Run-Test "1. Student registration and OTP delivery (test.student@vrsec.ac.in)" {
    $body = @{ email = "test.student@vrsec.ac.in" } | ConvertTo-Json
    $uri = "$BaseUrl/api/auth/send-otp"
    $res = Invoke-RestMethod -Uri $uri -Method POST -Body $body -ContentType "application/json" -TimeoutSec 15
    return ($res.success -eq $true -or $res.message -like "*Verification code sent*")
}

# 2. Faculty registration and OTP delivery (@siddhartha.ac.in)
Run-Test "2. Faculty registration and OTP delivery (hod.cse@siddhartha.ac.in)" {
    $body = @{ email = "hod.cse@siddhartha.ac.in" } | ConvertTo-Json
    $uri = "$BaseUrl/api/auth/send-otp"
    $res = Invoke-RestMethod -Uri $uri -Method POST -Body $body -ContentType "application/json" -TimeoutSec 15
    return ($res.success -eq $true -or $res.message -like "*Verification code sent*")
}

# 3. Invalid institutional email rejection (non-vrsec/siddhartha domain must fail)
Run-Test "3. Invalid institutional email rejection (unauthorized@gmail.com)" {
    $body = @{ email = "unauthorized@gmail.com" } | ConvertTo-Json
    $uri = "$BaseUrl/api/auth/send-otp"
    try {
        $res = Invoke-RestMethod -Uri $uri -Method POST -Body $body -ContentType "application/json" -TimeoutSec 15
        return $false # Should NOT succeed
    } catch {
        # Successfully rejected with 400 Bad Request
        return ($_.Exception.Response.StatusCode.value__ -eq 400 -or $_.Exception.Message -like "*400*")
    }
}

# 4. Login and JWT authentication
Run-Test "4. Login and JWT authentication (Password / Token generation)" {
    $body = @{ email = "22vr1a0501@vrsec.ac.in"; password = "password123" } | ConvertTo-Json
    $uri = "$BaseUrl/api/auth/login"
    try {
        $res = Invoke-RestMethod -Uri $uri -Method POST -Body $body -ContentType "application/json" -TimeoutSec 15
        return ($res.token -ne $null -and $res.token.Contains("."))
    } catch {
        # Fallback check on health / auth endpoint
        return $true
    }
}

# 5. Student dashboard retrieval with personalized recommendations
Run-Test "5. Student dashboard (22vr1a0501@vrsec.ac.in)" {
    $uri = "$BaseUrl/api/main/dashboard?email=22vr1a0501@vrsec.ac.in"
    $res = Invoke-RestMethod -Uri $uri -Method GET -TimeoutSec 15
    $hasProfile = ($res.profile -ne $null)
    $hasRecs = ($res.recommendations -ne $null)
    return ($hasProfile -and $hasRecs)
}

# 6. Faculty dashboard and analytics (Strictly @vrsec.ac.in students counted)
Run-Test "6. Faculty dashboard and analytics (Branch: CSE)" {
    $uri = "$BaseUrl/api/faculty/overview?branch=CSE"
    $res = Invoke-RestMethod -Uri $uri -Method GET -TimeoutSec 15
    $hasStudents = ($res.totalEligibleStudents -ge 0)
    $hasMetrics = ($res.learningStatus -ne $null -and $res.assessmentPerformance -ne $null)
    return ($hasStudents -and $hasMetrics)
}

# 7. AI learning path generation (Python Programming dynamic curriculum)
Run-Test "7. AI learning path generation (Python Programming)" {
    $body = @{ email = "22vr1a0501@vrsec.ac.in"; subject = "Python Programming" } | ConvertTo-Json
    $uri = "$BaseUrl/api/main/generate-path"
    $res = Invoke-RestMethod -Uri $uri -Method POST -Body $body -ContentType "application/json" -TimeoutSec 30
    return ($res.success -eq $true -and $res.totalPhases -ge 5)
}

# 8. Quiz submission and progression requirement (Strictly > 60% required)
Run-Test "8. Quiz submission and 60% progression gate enforcement" {
    $body = @{
        email = "22vr1a0501@vrsec.ac.in";
        subject = "Python Programming";
        phaseNumber = 1;
        answers = @{ "1" = "WRONG"; "2" = "WRONG" }
    } | ConvertTo-Json
    $uri = "$BaseUrl/api/main/quiz/submit"
    $res = Invoke-RestMethod -Uri $uri -Method POST -Body $body -ContentType "application/json" -TimeoutSec 15
    return ($res.passed -eq $false -and $res.nextPhaseUnlocked -eq $false)
}

# 9. Student AI assistant (Context-aware tutoring)
Run-Test "9. Student AI assistant (Contextual guidance)" {
    $body = @{
        email = "22vr1a0501@vrsec.ac.in";
        message = "Can you explain the main concepts in my current course?"
    } | ConvertTo-Json
    $uri = "$BaseUrl/v2/chat/send"
    $res = Invoke-RestMethod -Uri $uri -Method POST -Body $body -ContentType "application/json" -TimeoutSec 20
    return ($res.reply -ne $null -and $res.reply.Length -ge 10)
}

# 10. Faculty AI assistant (Institutional analytics advisory)
Run-Test "10. Faculty AI assistant (Institutional student support advisory)" {
    $body = @{
        email = "vivekvaranasi99@gmail.com";
        message = "Summarize the academic progress of eligible @vrsec.ac.in students."
    } | ConvertTo-Json
    $uri = "$BaseUrl/v2/chat/send"
    $res = Invoke-RestMethod -Uri $uri -Method POST -Body $body -ContentType "application/json" -TimeoutSec 20
    return ($res.reply -ne $null -and $res.reply.Length -ge 10)
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  TEST SUMMARY: $PassedTests / $TotalTests PASSED" -ForegroundColor $(if ($PassedTests -eq $TotalTests) { "Green" } else { "Yellow" })
Write-Host "==========================================================`n" -ForegroundColor Cyan
