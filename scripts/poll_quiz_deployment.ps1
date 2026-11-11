Write-Host "Monitoring quiz endpoint until commit 0967393 is deployed..."
$maxAttempts = 30
$attempt = 1

while ($attempt -le $maxAttempts) {
    try {
        $res = Invoke-RestMethod -Uri "https://ai-learning-backend-ztk7.onrender.com/api/main/quiz/generate?courseId=1&phaseNumber=1" -Method GET -TimeoutSec 15
        if ($res.totalQuestions -ne $null) {
            Write-Host "SUCCESS! Quiz questions generated! Total: $($res.totalQuestions)" -ForegroundColor Green
            $res | ConvertTo-Json -Depth 4
            break
        }
    } catch {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $msg = $reader.ReadToEnd()
        Write-Host "Attempt ${attempt} ($(Get-Date -Format 'HH:mm:ss')): Still getting previous response or deploying. Msg: $msg"
        Start-Sleep -Seconds 15
    }
    $attempt++
}
