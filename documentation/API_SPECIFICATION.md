# LearnPath AI — REST API Specification

**Base URL**: `https://ai-learning-backend-ztk7.onrender.com`  
**Protocol**: HTTPS  
**Data Format**: JSON (`Content-Type: application/json`)

---

## 1. Authentication & Identity (`/api/auth`)

### 1.1 Send Email OTP
- **Method**: `POST /api/auth/send-otp`
- **Request Body**:
```json
{
  "email": "student@vrsec.ac.in"
}
```
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Verification code sent to student@vrsec.ac.in",
  "email": "student@vrsec.ac.in",
  "dispatched": true
}
```

### 1.2 Verify Email OTP & Login / Register
- **Method**: `POST /api/auth/verify-otp`
- **Request Body**:
```json
{
  "email": "student@vrsec.ac.in",
  "otp": "492019",
  "username": "Vivek Varanasi"
}
```
- **Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "student@vrsec.ac.in",
    "username": "Vivek Varanasi",
    "role": "STUDENT",
    "branch": "CSE",
    "academicYear": "2nd Year",
    "semester": "1st Sem",
    "cgpa": 8.5
  },
  "role": "STUDENT"
}
```

### 1.3 Google OAuth Authentication
- **Method**: `POST /api/auth/google`
- **Request Body**:
```json
{
  "idToken": "<google_oauth_id_token>"
}
```
- **Response** (200 OK): Returns authenticated user object and JWT bearer token.

---

## 2. Student Dashboard & Curriculum (`/api/main`)

### 2.1 Get Student Dashboard
- **Method**: `GET /api/main/dashboard?email={email}`
- **Response** (200 OK):
```json
{
  "profile": {
    "id": 1,
    "email": "student@vrsec.ac.in",
    "username": "Vivek Varanasi",
    "role": "STUDENT",
    "branch": "CSE",
    "academicYear": "2nd Year",
    "semester": "1st Sem",
    "cgpa": 8.5
  },
  "activeCourses": [
    {
      "id": 101,
      "title": "Data Structures & Algorithmic Complexity",
      "progress": 25,
      "currentPhase": 2,
      "totalPhases": 8,
      "phases": [...]
    }
  ],
  "recommendations": [
    {
      "title": "Operating Systems & Kernel Process Modeling",
      "description": "Core 2nd Year Sem 2 architecture.",
      "difficulty": "Intermediate",
      "duration": "4 Weeks",
      "isFacultyAssigned": false
    }
  ],
  "weakTopics": []
}
```

### 2.2 Generate Subject-Specific Learning Path
- **Method**: `POST /api/main/generate-path`
- **Request Body**:
```json
{
  "email": "student@vrsec.ac.in",
  "subject": "Python Programming"
}
```
- **Response** (200 OK): Generates dynamic 6-8 phases, saves to Supabase, unlocks Phase 1.

---

## 3. 10-Question Quiz Engine & Automatic Phase Unlocking (`/api/main/quiz`)

### 3.1 Generate Phase Quiz
- **Method**: `GET /api/main/quiz/generate?courseId={courseId}&phaseNumber={phaseNumber}`
- **Response** (200 OK):
```json
{
  "courseId": 101,
  "phaseNumber": 1,
  "totalQuestions": 10,
  "questions": [
    {
      "id": 501,
      "questionIndex": 1,
      "questionText": "What is the worst-case time complexity of quicksort?",
      "options": ["O(N log N)", "O(N^2)", "O(log N)", "O(1)"],
      "conceptTag": "Algorithmic Complexity"
    }
  ]
}
```
*Note: `correctOption` is withheld by the backend to prevent inspection attacks.*

### 3.2 Submit Quiz Answers
- **Method**: `POST /api/main/quiz/submit`
- **Request Body**:
```json
{
  "email": "student@vrsec.ac.in",
  "courseId": 101,
  "phaseNumber": 1,
  "answers": {
    "501": "B",
    "502": "A",
    "503": "C"
  }
}
```
- **Response** (200 OK):
```json
{
  "passed": true,
  "score": 8,
  "totalQuestions": 10,
  "percentage": 80.0,
  "nextPhaseUnlocked": true,
  "feedback": "Outstanding work! You scored 8/10 (80%). The next phase has been unlocked!",
  "review": [...]
}
```

---

## 4. Faculty Department Portal (`/api/faculty`)

### 4.1 Faculty Overview (Strictly `@vrsec.ac.in` Students)
- **Method**: `GET /api/faculty/overview?branch=CSE`
- **Response** (200 OK):
```json
{
  "totalEligibleStudents": 45,
  "activeLearners": 42,
  "completedCourses": 12,
  "studentsNeedingSupportCount": 3,
  "branchDistribution": { "CSE": 45 },
  "studentsNeedingSupport": [...]
}
```

### 4.2 Assign Recommended Course to Cohort
- **Method**: `POST /api/faculty/recommend`
- **Request Body**:
```json
{
  "title": "Data Structures and Algorithms",
  "description": "Essential placement preparation curriculum.",
  "branch": "CSE",
  "year": "2nd Year",
  "semester": "1st Sem",
  "facultyName": "Prof. Vivek Varanasi",
  "facultyEmail": "vivekvaranasi99@gmail.com",
  "priority": "HIGH"
}
```

---

## 5. AI Chat Hub (`/v2/chat`)

### 5.1 Send Message (Student AI or Faculty AI)
- **Method**: `POST /v2/chat/send`
- **Request Body**:
```json
{
  "email": "student@vrsec.ac.in",
  "sessionId": 1731149200000,
  "message": "Explain recursion in simple terms with an example"
}
```
- **Response** (200 OK):
```json
{
  "sessionId": 1731149200000,
  "reply": "Recursion is a technique where a function solves a problem by calling a smaller instance of itself...",
  "title": "Recursion Explanation"
}
```
