$BaseUrl = "https://ai-learning-backend-ztk7.onrender.com"
$email = "test.student@vrsec.ac.in"

Write-Host "--- PHASE 2 QUIZ GENERATION & UNLOCKING VERIFICATION ---" -ForegroundColor Cyan

# 1. Generate Phase 2 Quiz
Write-Host "[1] Generating / Fetching Phase 2 Quiz..."
$quiz2 = Invoke-RestMethod -Uri "$BaseUrl/api/main/quiz/generate?courseId=1&phaseNumber=2" -Method GET
Write-Host "Total Questions: $($quiz2.totalQuestions)"
Write-Host "Q1: $($quiz2.questions[0].questionText)"
Write-Host "Concept: $($quiz2.questions[0].conceptTag)"

# 2. Check Phase 3 before submission
$phase3Before = Invoke-RestMethod -Uri "$BaseUrl/api/main/phase/details?courseId=1&phaseNumber=3" -Method GET
Write-Host "Phase 3 isUnlocked BEFORE passing Phase 2: $($phase3Before.isUnlocked)"

# 3. Submit passing attempt for Phase 2
$answers2 = @{}
foreach ($q in $quiz2.questions) {
    $answers2[$q.id.ToString()] = "A"
}
$body2 = @{
    email = $email
    courseId = 1
    phaseNumber = 2
    answers = $answers2
} | ConvertTo-Json

$passRes2 = Invoke-RestMethod -Uri "$BaseUrl/api/main/quiz/submit" -Method POST -Headers @{"Content-Type"="application/json"} -Body $body2
Write-Host "`nPhase 2 Submission Result:"
Write-Host "  Passed: $($passRes2.passed)"
Write-Host "  Score: $($passRes2.score)/$($passRes2.totalQuestions)"
Write-Host "  Percentage: $($passRes2.percentage)%"
Write-Host "  Next Phase Unlocked: $($passRes2.nextPhaseUnlocked)"
Write-Host "  Feedback: $($passRes2.feedback)"

# 4. Check Phase 3 after submission
$phase3After = Invoke-RestMethod -Uri "$BaseUrl/api/main/phase/details?courseId=1&phaseNumber=3" -Method GET
Write-Host "`nPhase 3 isUnlocked AFTER passing Phase 2: $($phase3After.isUnlocked)"

# 5. Check Dashboard
$dash = Invoke-RestMethod -Uri "$BaseUrl/api/main/dashboard?email=$email" -Method GET
$pyCourse = $dash.activeCourses | Where-Object { $_.id -eq 1 }
Write-Host "`nDashboard State:"
Write-Host "  Course: $($pyCourse.title)"
Write-Host "  Current Phase: $($pyCourse.currentPhase) / $($pyCourse.totalPhases)"
Write-Host "  Overall Progress: $($pyCourse.progress)%"
Write-Host "  Total Completed Courses: $($dash.totalCompletedCourses)"
Write-Host "--------------------------------------------------------"
