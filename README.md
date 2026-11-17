# LearnPath AI — AI-Powered Personalized Learning Path Generator

[![TypeScript](https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![React Native](https://img.shields.io/badge/React_Native-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://reactnative.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Supabase](https://img.shields.io/badge/Supabase-181818?style=for-the-badge&logo=supabase&logoColor=white)](https://supabase.com/)
[![Google Gemini](https://img.shields.io/badge/Google_Gemini-8E75C2?style=for-the-badge&logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)

LearnPath AI is an advanced university-grade personalized learning platform designed to dynamically generate individualized course curriculums, assess concept mastery through 10-question multiple-choice quizzes, adaptively detect weak concepts, and provide institutional academic performance analytics to faculty.

---

## 🌟 Key Features

### 🎓 Student Experience
- **Dynamic Learning Paths**: No fixed templates. Courses are dynamically generated with 5 to 8 subject-specific phases (learning objectives, topic explanations, subtopics, code examples, practical applications).
- **10-Question Phase Quizzes**: Exactly 10 questions per phase. Correct answers are kept secure on the server.
- **Strict Progression Gate**: Students must score strictly greater than 60% (at least 7/10) to automatically unlock the next phase.
- **Adaptive Learning Engine**: Tracks incorrect concept tags in `topic_performance` and provides remedial revision recommendations.
- **Personalized Student AI Mentor**: Context-aware AI tutor equipped with the student's real-time course progress, quiz scores, and weak topics.
- **Explore Custom Courses**: Input any subject ("What would you like to learn today?") to instantly generate an adaptive roadmap.

### 🏛️ Faculty Experience
- **Isolated VRSEC Student Analytics**: Exclusively monitors verified `@vrsec.ac.in` student learners. Faculty accounts are never counted as students.
- **Needs Attention Detection**: Identifies slow learners and struggling students based on repeated quiz scores &lt;= 60% and CGPA &lt; 6.5.
- **Targeted Course Assignment**: Recommend courses to specific cohorts (Branch, Academic Year, Semester). Only eligible matching students receive them.
- **Institutional Faculty AI**: Executive advisory assistant that answers queries on branch pass rates, struggling topics, and cohort trends.

---

## 🏗️ Repository Architecture

```
LearnPath-AI/
├── frontend/             # React Native 0.87.1 Mobile Client (React 19, TypeScript)
│   ├── src/
│   │   ├── components/   # Vector SVG illustrations & icons
│   │   ├── config/       # API Base URL configuration
│   │   ├── screens/      # Student & Faculty redesigned screens
│   │   ├── services/     # Unified, typed API client
│   │   └── styles/       # Professional Soft Blue design system
│   ├── App.tsx
│   └── package.json
├── backend/              # Spring Boot 4.x / Java 17 Production API
│   ├── src/main/java/com/vrsec/backend/
│   │   ├── controller/   # REST API Controllers (Auth, Student, Quiz, Faculty, Chat)
│   │   ├── model/        # JPA Entities (User, Course, Phase, Quiz, TopicPerf, Chat)
│   │   ├── repository/   # Spring Data Repositories
│   │   └── service/      # Business Logic, Gemini AI, Email OTP, JWT
│   ├── Dockerfile        # Production Dockerfile for Render
│   └── pom.xml
├── database/             # Supabase PostgreSQL Database Assets
│   ├── 01_schema.sql     # Complete DDL tables, indexes, and constraints
│   ├── 02_rls_policies.sql # Supabase Row Level Security policies
│   └── 03_seed_data.sql  # Verified test accounts & recommendations
├── documentation/        # Technical Specifications & Setup Guides
│   ├── ARCHITECTURE.md
│   ├── API_SPECIFICATION.md
│   ├── DATABASE_SCHEMA.md
│   ├── DEPLOYMENT_RENDER.md
│   └── FRONTEND_GUIDE.md
├── scripts/              # Verification & deployment helper scripts
├── .gitignore
├── .env.example
└── README.md
```

---

## 🚀 Quick Setup & Installation

### 1. Backend Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/vvsvivek/LearnPath-AI.git
   cd LearnPath-AI/backend
   ```
2. Configure environment variables in `.env` (refer to `.env.example`).
3. Run with Maven:
   ```bash
   ./mvnw spring-boot:run
   ```

### 2. Frontend Setup
1. Navigate to `frontend`:
   ```bash
   cd ../frontend
   npm install
   ```
2. Verify TypeScript integrity:
   ```bash
   npx tsc --noEmit
   ```
3. Start Metro bundler:
   ```bash
   npm start
   ```

### 3. Database Setup (Supabase)
1. Open the Supabase SQL Editor for your project (`munzcpabkjiuxorgmrse`).
2. Run `database/01_schema.sql` to verify tables and constraints.
3. Run `database/02_rls_policies.sql` to enable Row Level Security.
4. Run `database/03_seed_data.sql` to populate initial seed records.

---

## 🔐 Security & Access Control Summary

| Entity | Registration Rule | Role Enforcement | Analytics Inclusion |
| :--- | :--- | :--- | :--- |
| **Faculty** | `@siddhartha.ac.in` or `vivekvaranasi99@gmail.com` | Evaluated server-side; signed in JWT | Excluded from student metrics |
| **Eligible Student** | `@vrsec.ac.in` | Automatically assigned `STUDENT` | Included in faculty dashboard & recommendations |
| **General Student** | Any valid email address | Automatically assigned `STUDENT` | Private learning path; isolated from faculty metrics |

---

## 📄 License
This project is proprietary and developed for LearnPath AI.
