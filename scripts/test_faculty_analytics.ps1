$BaseUrl = "https://ai-learning-backend-ztk7.onrender.com"

Write-Host "=== VERIFYING FACULTY ANALYTICS ISOLATION ===" -ForegroundColor Cyan
$overview = Invoke-RestMethod -Uri "$BaseUrl/api/faculty/overview" -Method GET
Write-Host "Faculty Overview:"
$overview | ConvertTo-Json

$students = Invoke-RestMethod -Uri "$BaseUrl/api/faculty/students" -Method GET
Write-Host "`nTotal Students in Analytics: $($students.Count)"

$facultyInStudents = $students | Where-Object { 
    $_.email -like "*@siddhartha.ac.in" -or 
    $_.email -eq "vivekvaranasi99@gmail.com" -or 
    $_.email -eq "vijayasaivivek@gmail.com" 
}

Write-Host "Faculty accounts contaminating student list: $(if ($facultyInStudents) { $facultyInStudents.Count } else { 0 })"
if (-not $facultyInStudents -or $facultyInStudents.Count -eq 0) {
    Write-Host "SUCCESS: 100% strict isolation confirmed! Faculty accounts are fully excluded from student analytics." -ForegroundColor Green
} else {
    Write-Host "WARNING: Contamination detected!" -ForegroundColor Red
}
