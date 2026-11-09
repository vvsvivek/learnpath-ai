# LearnPath AI — Architecture Documentation

## Overview
**LearnPath AI** is an advanced, production-ready, AI-driven educational platform engineered to generate personalized learning paths for university engineering students, evaluate concept mastery via dynamic 10-question quizzes, and provide real-time departmental intelligence to university faculty.

---

## 1. System Topology & Component Layout

```mermaid
flowchart TD
    subgraph Client ["Frontend Client (React Native 0.87.1)"]
        UI_Student["Student Experience\n- Personalized Recommendations\n- Explore Custom Topics\n- Dynamic Learning Paths\n- 10-Question Quizzes\n- Context-Aware AI Tutor"]
        UI_Faculty["Faculty Experience\n- @vrsec.ac.in Analytics\n- Needs Attention Detection\n- Cohort Course Assignment\n- Faculty Institutional AI"]
        API_Client["Unified API Service (api.ts)\nJWT Bearer + Failover"]
    end

    subgraph CloudBackend ["Cloud Backend (Spring Boot 4.x / Java 17)"]
        AuthCtrl["AuthController\n- Real Email OTP\n- Domain Role Assignment\n- Google OAuth"]
        ApiCtrl["ApiController\n- Student Dashboard\n- Custom Curriculum Engine\n- Phase Content"]
        QuizCtrl["QuizController\n- 10-Question Generation\n- Hidden Correct Options\n- >60% Progression Gate"]
        FacCtrl["FacultyController\n- Strict @vrsec.ac.in Isolation\n- Slow Learner Detection\n- Course Broadcasting"]
        ChatCtrl["ChatController\n- Student Context Ingestion\n- Faculty Analytics Ingestion\n- Conversation Persistence"]
        
        LPService["LearningPathService\nCurriculum & Quiz Engine"]
        AIService["AiAssistantService\nContextual Gemini Pipeline"]
        EmailService["EmailService\nBrevo + Resend + SMTP Relay"]
    end

    subgraph DataTier ["Data Tier (Supabase PostgreSQL)"]
        Users["users table"]
        Courses["courses & course_phases"]
        Quizzes["quiz_questions & quiz_attempts"]
        TopicPerf["topic_performance (Adaptive)"]
        ChatDB["chat_sessions & chat_messages"]
        RLS["Row Level Security Policies"]
    end

    subgraph ExternalServices ["External Cloud Services"]
        Gemini["Google Gemini Generative AI"]
        Brevo["Brevo / Resend Email Delivery"]
        Render["Render Cloud Hosting"]
    end

    UI_Student --> API_Client
    UI_Faculty --> API_Client
    API_Client --> AuthCtrl & ApiCtrl & QuizCtrl & FacCtrl & ChatCtrl
    
    AuthCtrl --> EmailService
    ApiCtrl --> LPService
    QuizCtrl --> LPService
    ChatCtrl --> AIService
    
    LPService --> Gemini
    AIService --> Gemini
    EmailService --> Brevo

    LPService & AIService & AuthCtrl & FacCtrl --> DataTier
```

---

## 2. Security Architecture & Role Enforcement

### 2.1 Server-Side Role Assignment
- **Student Registration**: Open to any valid email address (`@vrsec.ac.in` or general).
- **Faculty Registration**: Restricted exclusively to:
  - Any email ending in `@siddhartha.ac.in`
  - `vivekvaranasi99@gmail.com`
- **Zero Client Trust**: Users can NEVER select or alter their role in request payloads. The backend evaluates email domains during verification and signs the calculated role into a cryptographic JWT.

### 2.2 Institutional Analytics Boundary
- Faculty dashboards and reports strictly query accounts ending in `@vrsec.ac.in`.
- Faculty accounts are never counted as students.
- General learners registered with other domains are isolated from faculty departmental tracking.

### 2.3 Assessment Integrity & Hidden Correct Options
- Quiz questions are generated dynamically (10 questions per phase).
- **CRITICAL**: The correct answers (`correctOption`) and concept explanations are stored securely on the backend in `quiz_questions` and are **never** returned to the frontend before quiz submission.
- The client submits only its chosen options (`{ questionId: selectedOption }`).
- Server computes the score and enforces the progression rule:
  - `Score > 6/10 (> 60%)`: Phase marked completed, next phase unlocked, course progress updated.
  - `Score <= 6/10 (<= 60%)`: Next phase remains locked, concept weaknesses recorded in `topic_performance`, remedial materials recommended.

---

## 3. Adaptive Learning Feedback Loop

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant Client as React Native Client
    participant Backend as Spring Boot API
    participant AI as Google Gemini API
    participant DB as Supabase PostgreSQL

    Student->>Client: Completes Phase Study Material
    Student->>Client: Taps "Take Phase Quiz"
    Client->>Backend: GET /api/main/quiz/generate?courseId=...&phaseNumber=1
    Backend->>AI: Generate 10 specialized MCQs based on phase concepts
    AI-->>Backend: Return 10 questions with options & correct answers
    Backend->>DB: Store in quiz_questions table
    Backend-->>Client: Return 10 questions (WITHOUT correctOption)
    
    Student->>Client: Answers all 10 questions & Taps Submit
    Client->>Backend: POST /api/main/quiz/submit { answers: {...} }
    Backend->>DB: Fetch stored correctOption for questions
    Backend->>Backend: Calculate score & evaluate > 60% rule
    
    alt Score >= 7 / 10 (> 60%)
        Backend->>DB: Mark Phase Completed & Unlock Phase 2
        Backend-->>Client: Passed! Next Phase Unlocked + Full Review
    else Score <= 6 / 10 (<= 60%)
        Backend->>DB: Record Weak Concepts in topic_performance
        Backend-->>Client: Phase Remains Locked + Remedial Revision Recommended
    end
```
