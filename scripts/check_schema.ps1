try {
    $res = Invoke-RestMethod -Uri "https://ai-learning-backend-ztk7.onrender.com/api/main/quiz/schema" -Method GET -TimeoutSec 20
    Write-Host "SCHEMA ENDPOINT RESPONSE:"
    $res | ConvertTo-Json -Depth 6
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    if ($stream) {
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Error Body:" $reader.ReadToEnd()
    } else {
        Write-Host "Error:" $_.Exception.Message
    }
}
