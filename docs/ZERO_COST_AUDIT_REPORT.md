# Zero-Cost Infrastructure Audit Report

## 1. Project Overview & Cost Constraint Verification
- **App Name:** AI-Powered Personalized Learning Path Generator
- **Package ID:** `com.vrsec.learnpathai`
- **Total Project Budget:** **₹0 (Zero Rupees)**
- **Audit Date:** November 6, 2026
- **Result:** **100% Zero-Cost Compliant**

---

## 2. External Services Matrix

| Service | Component | Plan Selected | Free Tier Limits | Active Cost | Automatic Billing Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Supabase** | PostgreSQL Database | **Free Tier** | 500 MB database, 2 projects, 50k monthly active users | **₹0.00** | Disabled (No card required) |
| **Render** | FastAPI Backend API | **Free Web Service** | 750 hours/month (covers full 24/7 single service), 512 MB RAM | **₹0.00** | Disabled (No card required) |
| **Google Gemini** | AI Tutor, Paths, Quizzes | **Gemini Free Tier** | 15 Requests/Min (RPM), 1,500 Requests/Day (RPD), 1M Tokens/Min | **₹0.00** | Disabled (Google AI Studio Free API Key) |
| **Brevo (Sendinblue)** | Transactional OTP Email | **Free Plan** | 300 emails/day, single sender verification (Gmail works) | **₹0.00** | Disabled (No payment method) |
| **Expo / EAS** | Android APK / AAB Build | **Free Tier / Local** | 15 cloud builds/month OR unlimited free local builds (`--local`) | **₹0.00** | Free tier account |
| **GitHub** | Source Control & Backup | **Free Public/Private** | Unlimited public/private repositories, Git LFS free tier | **₹0.00** | Free plan |

---

## 3. Detailed Service Breakdown & Mitigation Strategies

### A. Database: Supabase PostgreSQL (Free Tier)
- **Quota Limit:** 500 MB database storage.
- **Inactivity Policy:** Free-tier Supabase projects automatically pause after 7 consecutive days of inactivity.
- **Mitigation & Protection:**
  1. The backend application runs periodic health checks or an external free cron (e.g. GitHub Actions / cron-job.org) can ping the `/health` endpoint which executes a lightweight `SELECT 1` query.
  2. Data storage is minimized: only student profiles, courses, questions, and attempt logs are stored. Large media is not hosted directly on the DB.
- **Card/Billing Risk:** None. Supabase does not enforce automatic upgrade when limits are reached; it blocks additional inserts until space is cleared.

### B. Hosting: Render Free Web Service
- **Quota Limit:** 750 free instance hours per month. Spins down after 15 minutes of inactivity. Cold start wake-up latency: ~30-50 seconds.
- **Mitigation & Protection:**
  1. Frontend incorporates an animated `ServerWakeup.tsx` banner and exponential backoff retry interceptor in `api.ts`.
  2. User is informed with a loading status ("Connecting to server...") rather than receiving a crash or blank screen.
  3. No credit card is required for Render free web services.
  4. Render Free PostgreSQL was deliberately **avoided** because Render deletes free databases after 30 days. Supabase is used instead.

### C. Artificial Intelligence: Google Gemini API (Free Tier)
- **Quota Limit:** 15 Requests Per Minute (RPM), 1,500 Requests Per Day (RPD).
- **Mitigation & Protection:**
  1. AI responses (course recommendations, 4-phase learning paths, generated quizzes) are stored persistently in Supabase so subsequent views do not re-trigger Gemini API calls.
  2. In-memory and database caching prevents redundant calls.
  3. AI chat messages have rate-limiting.
  4. Quota depletion fails gracefully with fallbacks and retry warnings.

### D. Email Dispatch: Brevo (Free Tier)
- **Quota Limit:** 300 emails per day.
- **Domain Requirement:** **₹0** (no custom domain purchase needed). The sender email (`vivekvaranasi99@gmail.com`) can be verified directly in the Brevo dashboard with a verification link.
- **Mitigation & Protection:**
  1. OTP rate limiting (maximum 5 requests per 5 minutes per email) prevents abuse.
  2. Dual-layer fallback: SDK API call with automatic fallback to Brevo HTTP REST v3 API.
  3. If quota (300/day) is ever exhausted during development, backend logs the plain OTP to console/terminal output.

### E. Mobile Build System: Expo EAS
- **Quota Limit:** 15 free cloud builds per month.
- **Mitigation & Protection:**
  1. Local builds can be generated with zero cost and unlimited quota using `npx eas-cli build --platform android --profile preview --local`.
  2. Preview APK profile configured in `eas.json` for testing without Play Store.

---

## 4. Play Store Mandatory Fee Clarification
- **Google Play Developer Account:** One-time \$25 fee (approx. ₹2,100) charged directly by Google to publish apps to Google Play Store. This was explicitly authorized by the user and is the only allowed expense.

---

## 5. Usage Monitoring Checklist to Avoid Charges
1. **Never attach a credit or debit card** to Supabase, Render, Google AI Studio, or Brevo.
2. In Google AI Studio, ensure billing account linking is **OFF**.
3. In Brevo, leave account on default "Free" tier (do not purchase SMS or email credits).
4. In Render, select only the **Free** instance type for web services.
5. In Supabase, monitor database usage under `Settings > Usage`.
