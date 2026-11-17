Write-Host "Waiting for Render to deploy commit 0967393..."
$ready = $false
$maxAttempts = 30
$attempt = 1

while (-not $ready -and $attempt -le $maxAttempts) {
    try {
        $res = Invoke-RestMethod -Uri "https://ai-learning-backend-ztk7.onrender.com/api/faculty/overview" -Method GET -TimeoutSec 10
        if ($res -ne $null -and $res.totalEligibleStudents -ne $null) {
            $ready = $true
            Write-Host "Render is LIVE! (Attempt ${attempt})"
            break
        }
    } catch {
        Write-Host "Attempt ${attempt}: Service still deploying/rebuilding... waiting 10s."
        Start-Sleep -Seconds 10
    }
    $attempt++
}

if (-not $ready) {
    Write-Host "Service did not respond within timeout." -ForegroundColor Red
}
