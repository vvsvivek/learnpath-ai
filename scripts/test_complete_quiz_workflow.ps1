$BaseUrl = "https://ai-learning-backend-ztk7.onrender.com"
$studentEmail = "test.student@vrsec.ac.in"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  LEARNPATH AI - COMPLETE QUIZ WORKFLOW VERIFICATION" -ForegroundColor Cyan
Write-Host "  Student: $studentEmail" -ForegroundColor Cyan
Write-Host "==========================================================`n" -ForegroundColor Cyan

# 1. Course Enrollment / Select Course
Write-Host "[1] Enrolling / selecting 'Python Programming' course..."
$selectBody = @{
    email = $studentEmail
    subject = "Python Programming"
} | ConvertTo-Json

$courseRes = Invoke-RestMethod -Uri "$BaseUrl/api/main/course/select" -Method POST -Headers @{"Content-Type"="application/json"} -Body $selectBody
$courseId = $courseRes.courseId
Write-Host "Course ID: $courseId, Title: $($courseRes.title), Total Phases: $($courseRes.totalPhases)"

# 2. Verify Phase 1 is unlocked and Phase 2 is locked before quiz
Write-Host "`n[2] Checking initial phase status..."
$phase1Initial = Invoke-RestMethod -Uri "$BaseUrl/api/main/phase/details?courseId=$courseId&phaseNumber=1" -Method GET
$phase2Initial = Invoke-RestMethod -Uri "$BaseUrl/api/main/phase/details?courseId=$courseId&phaseNumber=2" -Method GET
Write-Host "Phase 1 - isUnlocked: $($phase1Initial.isUnlocked), isCompleted: $($phase1Initial.isCompleted)"
Write-Host "Phase 2 - isUnlocked: $($phase2Initial.isUnlocked), isCompleted: $($phase2Initial.isCompleted)"

# 3. Generate exactly 10 questions for Phase 1
Write-Host "`n[3] Generating / Fetching Phase 1 Quiz (10 MCQs)..."
$quizRes = Invoke-RestMethod -Uri "$BaseUrl/api/main/quiz/generate?courseId=$courseId&phaseNumber=1" -Method GET
Write-Host "Total Questions Returned: $($quizRes.totalQuestions)"
Write-Host "Questions Array Count: $($quizRes.questions.Count)"

if ($quizRes.totalQuestions -ne 10) {
    Write-Host "ERROR: Expected 10 questions, got $($quizRes.totalQuestions)" -ForegroundColor Red
} else {
    Write-Host "SUCCESS: Exactly 10 questions received." -ForegroundColor Green
}

$questions = $quizRes.questions
for ($i = 0; $i -lt [Math]::Min(3, $questions.Count); $i++) {
    Write-Host "  Q$($i+1): $($questions[$i].questionText)"
    Write-Host "     Options: $($questions[$i].options -join ' | ')"
    Write-Host "     Concept: $($questions[$i].conceptTag)"
}

# 4. Submit FAILING attempt (Score < 70% / 60% rule: submit deliberately wrong answers)
Write-Host "`n[4] Submitting FAILING quiz attempt (all wrong answers)..."
$failAnswers = @{}
foreach ($q in $questions) {
    $failAnswers[$q.id.ToString()] = "WRONG_OPTION"
}

$failSubmitBody = @{
    email = $studentEmail
    courseId = $courseId
    phaseNumber = 1
    answers = $failAnswers
} | ConvertTo-Json

$failResult = Invoke-RestMethod -Uri "$BaseUrl/api/main/quiz/submit" -Method POST -Headers @{"Content-Type"="application/json"} -Body $failSubmitBody
Write-Host "Failing Attempt Result:"
Write-Host "  Score: $($failResult.score)/$($failResult.total)"
Write-Host "  Percentage: $($failResult.percentage)%"
Write-Host "  Passed: $($failResult.passed)"
Write-Host "  Next Phase Unlocked: $($failResult.nextPhaseUnlocked)"
Write-Host "  Weak Concepts: $($failResult.weakConcepts -join ', ')"

# Verify Phase 2 remains LOCKED after failure
$phase2AfterFail = Invoke-RestMethod -Uri "$BaseUrl/api/main/phase/details?courseId=$courseId&phaseNumber=2" -Method GET
Write-Host "  Verification: Phase 2 isUnlocked after failing quiz = $($phase2AfterFail.isUnlocked) (Expected: False)"

# 5. Submit PASSING attempt (Score >= 70% / > 60% rule)
Write-Host "`n[5] Submitting PASSING quiz attempt..."
# Retrieve the correct options from the review of the previous attempt or from review items
$correctAnswers = @{}
if ($failResult.review -ne $null) {
    foreach ($item in $failResult.review) {
        $correctAnswers[$item.questionId.ToString()] = $item.correctOption
    }
} else {
    # Default fallback to option A if review not returned
    foreach ($q in $questions) {
        $correctAnswers[$q.id.ToString()] = "A"
    }
}

Write-Host "Identified correct options for submission: $($correctAnswers.Values -join ', ')"

$passSubmitBody = @{
    email = $studentEmail
    courseId = $courseId
    phaseNumber = 1
    answers = $correctAnswers
} | ConvertTo-Json

$passResult = Invoke-RestMethod -Uri "$BaseUrl/api/main/quiz/submit" -Method POST -Headers @{"Content-Type"="application/json"} -Body $passSubmitBody
Write-Host "Passing Attempt Result:"
Write-Host "  Score: $($passResult.score)/$($passResult.total)"
Write-Host "  Percentage: $($passResult.percentage)%"
Write-Host "  Passed: $($passResult.passed)"
Write-Host "  Next Phase Unlocked: $($passResult.nextPhaseUnlocked)"

# 6. Verify Phase 2 is now UNLOCKED and Phase 1 is COMPLETED
Write-Host "`n[6] Verifying phase progression after passing quiz..."
$phase1AfterPass = Invoke-RestMethod -Uri "$BaseUrl/api/main/phase/details?courseId=$courseId&phaseNumber=1" -Method GET
$phase2AfterPass = Invoke-RestMethod -Uri "$BaseUrl/api/main/phase/details?courseId=$courseId&phaseNumber=2" -Method GET
Write-Host "Phase 1 - isUnlocked: $($phase1AfterPass.isUnlocked), isCompleted: $($phase1AfterPass.isCompleted) (Expected: Completed=True)"
Write-Host "Phase 2 - isUnlocked: $($phase2AfterPass.isUnlocked), isCompleted: $($phase2AfterPass.isCompleted) (Expected: Unlocked=True)"

# 7. Fresh API request to student dashboard to verify persistence
Write-Host "`n[7] Verifying Supabase persistence via fresh Dashboard API request..."
$dashRes = Invoke-RestMethod -Uri "$BaseUrl/api/main/dashboard?email=$studentEmail" -Method GET
$activeCourse = $dashRes.activeCourses | Where-Object { $_.id -eq $courseId }
Write-Host "Dashboard Course: $($activeCourse.title)"
Write-Host "Dashboard Current Phase: $($activeCourse.currentPhase) / $($activeCourse.totalPhases)"
Write-Host "Dashboard Overall Progress: $($activeCourse.progress)%"
$completedCount = ($activeCourse.phases | Where-Object { $_.isCompleted -eq $true }).Count
Write-Host "Dashboard Completed Phases: $completedCount"
Write-Host "Dashboard Weak Topics Count: $($dashRes.weakTopics.Count)"

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  QUIZ WORKFLOW VERIFICATION COMPLETE" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
