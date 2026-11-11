Write-Host "Polling /api/main/quiz/schema until commit c66917d (topic_id nullable: YES) is live..."
$maxAttempts = 30
$attempt = 1

while ($attempt -le $maxAttempts) {
    try {
        $res = Invoke-RestMethod -Uri "https://ai-learning-backend-ztk7.onrender.com/api/main/quiz/schema" -Method GET -TimeoutSec 15
        if ($res -ne $null -and $res.tables -ne $null -and $res.tables.topic_performance -ne $null) {
            $topicIdCol = $res.tables.topic_performance | Where-Object { $_.column -eq 'topic_id' }
            if ($topicIdCol -ne $null -and $topicIdCol.nullable -eq 'YES') {
                Write-Host "SUCCESS! Commit c66917d is LIVE! topic_id is now nullable = YES!" -ForegroundColor Green
                break
            } else {
                Write-Host "Attempt ${attempt} ($(Get-Date -Format 'HH:mm:ss')): Server responded, but topic_id is still nullable=$($topicIdCol.nullable)..."
            }
        } else {
            Write-Host "Attempt ${attempt} ($(Get-Date -Format 'HH:mm:ss')): Waiting for valid response..."
        }
    } catch {
        Write-Host "Attempt ${attempt} ($(Get-Date -Format 'HH:mm:ss')): Waiting for deployment... ($($_.Exception.Message))"
    }
    Start-Sleep -Seconds 10
    $attempt++
}
