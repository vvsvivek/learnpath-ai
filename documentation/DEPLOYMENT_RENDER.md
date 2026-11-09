# LearnPath AI — Render Backend Production Deployment Guide

## Overview
This document guides deploying the **LearnPath AI** Spring Boot backend on **Render** (`https://render.com`) using the production Dockerfile.

---

## 1. Render Service Configuration

| Setting | Value |
| :--- | :--- |
| **Service Type** | Web Service |
| **Environment** | Docker |
| **Branch** | `main` |
| **Root Directory** | `backend` |
| **Dockerfile Path** | `./Dockerfile` |
| **Auto-Deploy** | Yes |
| **Port** | `5000` |
| **Health Check Path** | `/api/main/dashboard?email=healthcheck` (or `/actuator/health`) |

---

## 2. Production Environment Variables in Render Dashboard

Configure the following environment variables in the Render Service Settings (`Environment` tab):

```env
# 1. Server Configuration
PORT=5000

# 2. Supabase PostgreSQL Connection Pooler
DB_URL=jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres
DB_USER=postgres.munzcpabkjiuxorgmrse
DB_PASSWORD=<your_supabase_db_password>

# 3. Google Gemini AI API Key
GEMINI_API_KEY=<your_google_gemini_api_key>

# 4. Email Delivery (Brevo or Resend)
BREVO_API_KEY=<your_brevo_api_key>
RESEND_API_KEY=<your_resend_api_key>

# 5. JWT Secret Key
JWT_SECRET=LearnPathAISecretKeyForProductionTokenSigning20261109SecureKey
```

---

## 3. Dockerfile Build Pipeline
The repository uses a multi-stage Alpine Dockerfile:
1. **Stage 1 (Build)**: `eclipse-temurin:17-jdk-alpine` runs `./mvnw clean package -DskipTests` to package the executable `.jar`.
2. **Stage 2 (Runtime)**: `eclipse-temurin:17-jre-alpine` runs `java -jar app.jar` on port 5000 with a lightweight footprint.

---

## 4. Live Verification Commands
Once deployed on Render (`https://ai-learning-backend-ztk7.onrender.com`), verify the live endpoints:

```bash
# 1. Test Send OTP
curl -X POST https://ai-learning-backend-ztk7.onrender.com/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test.verify@vrsec.ac.in"}'

# 2. Test Faculty Overview
curl https://ai-learning-backend-ztk7.onrender.com/api/faculty/overview?branch=CSE
```
