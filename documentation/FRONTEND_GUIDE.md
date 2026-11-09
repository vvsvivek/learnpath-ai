# LearnPath AI — Frontend Architecture & Development Guide

## Overview
The **LearnPath AI** mobile frontend is built on **React Native 0.87.1** and **React 19**, designed using a professional **Soft Blue & Deep Navy** design system.

---

## 1. Directory Structure

```
frontend/
├── src/
│   ├── components/
│   │   └── Illustrations.tsx      # Vector SVG illustrations & icons
│   ├── config/
│   │   └── apiConfig.ts          # Central API URLs (Render + Local)
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── GetStartedScreen.tsx # Hero intro with walking student illustration
│   │   │   └── AuthScreen.tsx       # Email OTP & Google OAuth Login/Register
│   │   ├── main/
│   │   │   ├── HomeScreen.tsx       # Student Dashboard & Explore Courses
│   │   │   ├── LearningPathScreen.tsx # Dynamic phases & 10-question quiz
│   │   │   ├── FacultyHomeScreen.tsx  # Faculty Portal Greeting & Stats
│   │   │   ├── FacultyDashboardScreen.tsx # Cohort analytics & course assignment
│   │   │   ├── ChatScreen.tsx       # Student & Faculty AI Assistant
│   │   │   ├── ProfileScreen.tsx    # Academic profile & verified badges
│   │   │   └── MainLayout.tsx       # Bottom pill navigation layout
│   │   └── SettingsScreen.tsx       # App preferences & appearance theme
│   ├── services/
│   │   └── api.ts                   # Unified, typed API client
│   └── styles/
│       └── theme.ts                 # Cohesive color palette, shadows, radii
├── App.tsx                          # Root React Native component
└── package.json
```

---

## 2. Design System Tokens (`theme.ts`)

- **Primary Soft Blue**: `#4A90D9`
- **Deep Navy**: `#1E3A5F`
- **Soft Blue Tint**: `#EBF3FC`
- **Canvas Background**: `#F8FAFC`
- **Card Surface**: `#FFFFFF`
- **Slate Text**: `#0F172A`
- **Muted Text**: `#64748B`
- **Success (Passed)**: `#10B981`
- **Danger (Locked / Remedial)**: `#EF4444`
- **Warning (In Progress)**: `#F59E0B`

---

## 3. Running & Testing

```bash
cd frontend

# Run TypeScript type check
npx tsc --noEmit

# Start Metro Bundler
npm start

# Run on Android
npm run android

# Run on iOS
npm run ios
```
