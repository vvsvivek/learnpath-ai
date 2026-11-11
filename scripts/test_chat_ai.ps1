$BaseUrl = "https://ai-learning-backend-ztk7.onrender.com"
$email = "test.student@vrsec.ac.in"

Write-Host "--- TESTING AI CHATBOT WITH GEMINI ---" -ForegroundColor Cyan

# 1. Create a session
$newSession = Invoke-RestMethod -Uri "$BaseUrl/v2/chat/new?email=$email&contextType=STUDENT" -Method POST
$sessionId = $newSession.id
Write-Host "Created Session ID: $sessionId"

# 2. Send a query to Gemini
$body = @{
    email = $email
    sessionId = $sessionId
    message = "Explain what a Python generator is in 2 concise sentences."
} | ConvertTo-Json

$chatRes = Invoke-RestMethod -Uri "$BaseUrl/v2/chat/send" -Method POST -Headers @{"Content-Type"="application/json"} -Body $body
Write-Host "`nAI Tutor Response:"
Write-Host $chatRes.reply
Write-Host "-------------------------------------"
