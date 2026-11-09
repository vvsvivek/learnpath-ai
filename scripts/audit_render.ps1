param(
    [string]$BaseUrl = "https://ai-learning-backend-ztk7.onrender.com"
)

Write-Host "--- AUDITING LIVE RENDER SERVICE ---"
Write-Host "Target: $BaseUrl"

# 1. Check Root Info
try {
    $info = Invoke-RestMethod -Uri "$BaseUrl/" -Method GET -TimeoutSec 15
    Write-Host "Root Info:" ($info | ConvertTo-Json -Compress)
} catch {
    Write-Host "Root GET Error:" $_.Exception.Message
}

# 2. Check OpenAPI / Docs
try {
    $docs = Invoke-WebRequest -Uri "$BaseUrl/docs" -Method GET -UseBasicParsing -TimeoutSec 15
    Write-Host "Swagger /docs Status:" $docs.StatusCode
} catch {
    Write-Host "Swagger /docs Error:" $_.Exception.Message
}

# 3. Check Spring Boot endpoint vs FastAPI endpoint
Write-Host "`nTesting Spring Boot endpoint: /api/auth/send-otp"
try {
    $body = @{ email = "test.student@vrsec.ac.in" } | ConvertTo-Json
    $resSpring = Invoke-RestMethod -Uri "$BaseUrl/api/auth/send-otp" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 15
    Write-Host "Spring Boot /api/auth/send-otp Success:" ($resSpring | ConvertTo-Json -Compress)
} catch {
    Write-Host "Spring Boot /api/auth/send-otp Result:" $_.Exception.Message
}

Write-Host "`nTesting FastAPI endpoint: /auth/request-otp with full error capture"
try {
    $body = '{"email":"test.student@vrsec.ac.in"}'
    $req = [System.Net.HttpWebRequest]::Create("$BaseUrl/auth/request-otp")
    $req.Method = "POST"
    $req.ContentType = "application/json"
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $req.ContentLength = $bytes.Length
    $stream = $req.GetRequestStream()
    $stream.Write($bytes, 0, $bytes.Length)
    $stream.Close()
    $resp = $req.GetResponse()
    $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
    Write-Host "Success Response:" $sr.ReadToEnd()
} catch [System.Net.WebException] {
    Write-Host "HTTP Status:" $_.Exception.Response.StatusCode
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Server Error Body:" $reader.ReadToEnd()
    }
} catch {
    Write-Host "General Error:" $_.Exception.Message
}

Write-Host "`nTesting Spring Boot endpoint: /api/faculty/overview"
try {
    $resFacSpring = Invoke-RestMethod -Uri "$BaseUrl/api/faculty/overview?branch=CSE" -Method GET -TimeoutSec 15
    Write-Host "Spring Boot /api/faculty/overview Success:" ($resFacSpring | ConvertTo-Json -Compress)
} catch {
    Write-Host "Spring Boot /api/faculty/overview Result:" $_.Exception.Message
}
