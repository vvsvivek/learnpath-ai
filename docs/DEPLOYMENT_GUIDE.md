# Production Deployment & Setup Guide

This guide provides step-by-step instructions to deploy the AI Learning Path Generator backend and database on **100% Free Tier** services and build the Android application.

---

## 1. Supabase Database Setup (Free)

1. Sign up / Log in to [Supabase](https://supabase.com) (Free account, no credit card required).
2. Click **New Project** and name it `learnpath-ai`. Set a secure database password and choose your nearest region.
3. Once created, go to the **SQL Editor** tab on the left sidebar.
4. Open the SQL file located in this project:
   `backend/migrations/001_initial_schema.sql`
5. Copy the entire file content, paste it into the Supabase SQL Editor, and click **Run**.
   - This creates all 18 PostgreSQL tables, indexes, UUID generators, and foreign key relationships.
6. Go to **Project Settings > API**:
   - Copy **Project URL** (e.g. `https://xyzcompany.supabase.co`).
   - Copy **anon public** key.
   - Copy **service_role secret** key.

---

## 2. Google Gemini API Setup (Free)

1. Go to [Google AI Studio](https://aistudio.google.com).
2. Sign in with your Google account.
3. Click **Get API key** > **Create API key**.
4. Copy your key. (Ensure it stays on the Free Tier: 15 RPM / 1,500 RPD, billing disabled).

---

## 3. Brevo (Sendinblue) Email Setup (Free)

1. Go to [Brevo](https://www.brevo.com) and create a free account.
2. Under your profile icon (top right), click **Senders, Domains & Dedicated IPs**.
3. Click **Senders** > **Add a sender**.
4. Enter your email (e.g. `vivekvaranasi99@gmail.com`) and sender name `AI Learning Path`.
5. Check your inbox and click the verification link sent by Brevo.
6. Go to **SMTP & API** > **API Keys** > **Generate a new API key**.
7. Name it `learnpath-backend` and copy the key.

---

## 4. Render Backend Deployment (Free)

1. Sign up / Log in to [Render](https://render.com) (Free account).
2. Click **New +** > **Web Service**.
3. Connect your GitHub repository: `https://github.com/vvsvivek/learnpath-ai`.
4. Configure the Web Service:
   - **Name:** `learnpath-ai-api`
   - **Root Directory:** `backend`
   - **Environment:** `Python 3`
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
   - **Instance Type:** **Free** (0.5 CPU, 512 MB RAM)
5. Under **Environment Variables**, add:
   ```env
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-supabase-anon-key
   SUPABASE_SERVICE_ROLE_KEY=your-supabase-service-role-key
   BREVO_API_KEY=your-brevo-api-key
   BREVO_SENDER_EMAIL=vivekvaranasi99@gmail.com
   BREVO_SENDER_NAME=AI Learning Path
   GEMINI_API_KEY=your-gemini-api-key
   GEMINI_MODEL=gemini-2.5-flash
   JWT_SECRET_KEY=super-secret-jwt-key-minimum-32-characters-required-for-production
   FACULTY_ALLOWED_DOMAINS=["@siddhartha.ac.in"]
   FACULTY_SPECIAL_EMAILS=["vivekvaranasi99@gmail.com"]
   STUDENT_INSTITUTIONAL_DOMAIN=@vrsec.ac.in
   ENVIRONMENT=production
   ```
6. Click **Create Web Service**.
7. Once deployed, Render will provide a public URL:
   `https://learnpath-ai-api.onrender.com`
8. Verify it by opening `https://learnpath-ai-api.onrender.com/health` in your browser.

---

## 5. Frontend Mobile Configuration & Local Run

1. Open `frontend/.env.example` and create `frontend/.env`:
   ```env
   EXPO_PUBLIC_API_URL=https://learnpath-ai-api.onrender.com
   ```
   *(For testing on local Android emulator or phone before deploying to Render, use your computer's local IP, e.g. `http://192.168.1.100:8000`).*
2. Start the Expo app:
   ```bash
   cd frontend
   npx expo start
   ```
3. Scan the QR code using the **Expo Go** app on your Android phone, or press `a` to run on an Android emulator.

---

## 6. Building Android Release APK and AAB (Free)

### Option A: Free Cloud Build with EAS (15 builds/month)
1. Install EAS CLI:
   ```bash
   npm install -g eas-cli
   ```
2. Log in to your free Expo account:
   ```bash
   eas login
   ```
3. Build standalone APK for testing on physical devices:
   ```bash
   cd frontend
   eas build --platform android --profile preview
   ```
4. Build release Android App Bundle (AAB) for Google Play Store:
   ```bash
   eas build --platform android --profile production
   ```

### Option B: 100% Unlimited Free Local Builds
If you have Android Studio installed locally:
```bash
cd frontend
eas build --platform android --profile preview --local
```
This generates an `.apk` file directly in your project folder with no cloud queue and zero quota limits.

---

## 7. Google Play Store Submission Preparation

1. **Package Name:** `com.vrsec.learnpathai`
2. **App Title:** AI Personalized Learning
3. **Category:** Education
4. **Google Play Console Account:** \$25 one-time registration fee at [play.google.com/console](https://play.google.com/console).
5. **Testing Requirements (Google 2024+ Policy):**
   - For new personal developer accounts, Google requires closed testing with at least 12-20 testers for 14 consecutive days before production access is unlocked.
   - Use the **Closed Testing** track and invite classmates/faculty.
