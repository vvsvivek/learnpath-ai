try {
    $res = Invoke-RestMethod -Uri 'https://ai-learning-backend-ztk7.onrender.com/api/main/quiz/generate?courseId=1&phaseNumber=1' -Method GET
    $res | ConvertTo-Json -Depth 5
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "Response Body:" $reader.ReadToEnd()
}
