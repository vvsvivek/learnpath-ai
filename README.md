# AI Personalized Learning Path Generator

An AI-powered mobile application that generates personalized learning paths for B.Tech students, with intelligent course recommendations, dynamic quizzes, and comprehensive analytics for both students and faculty.

## 🏗️ Architecture

```
├── frontend/          # React Native (Expo SDK 58) mobile app
├── backend/           # Python FastAPI REST API server
├── docs/              # Documentation
└── README.md
```

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Mobile | React Native + Expo SDK 58 + TypeScript |
| Backend | Python FastAPI |
| Database | Supabase PostgreSQL |
| AI | Google Gemini (google-genai) |
| Email | Brevo (transactional OTP) |
| Hosting | Render (backend) |
| Builds | Expo EAS Build |

## 🚀 Quick Start

### Prerequisites
- Node.js 20+
- Python 3.12+
- Expo CLI (`npm install -g expo-cli`)
- Git

### Backend Setup

```bash
cd backend
python -m venv venv
# Windows
venv\Scripts\activate
# macOS/Linux
source venv/bin/activate

pip install -r requirements.txt
cp .env.example .env
# Fill in your environment variables in .env
uvicorn app.main:app --reload
```

### Frontend Setup

```bash
cd frontend
npm install
cp .env.example .env
# Fill in your environment variables in .env
npx expo start
```

## 🔐 Environment Variables

See `.env.example` files in both `backend/` and `frontend/` directories.

**Never commit `.env` files or API keys to the repository.**

## 📱 Features

### Student App
- 🔐 OTP-based email authentication
- 🏠 Personalized dashboard with learning stats
- 📚 AI-powered course recommendations
- 🗺️ 4-phase structured learning paths
- 📝 Dynamic AI-generated quizzes
- 🤖 Intelligent AI study assistant
- 📊 Performance analytics and reports
- ⏱️ Study session tracking
- 🌙 Dark mode support

### Faculty App
- 🔐 Domain-restricted authentication (@siddhartha.ac.in)
- 📊 Institutional student analytics
- 🎓 Student performance categorization
- 📋 Course assignment to student groups
- 🤖 Faculty AI assistant for analytics
- 📈 Interactive charts and reports

## 📄 License

MIT License

## 👤 Author

Vivek Varanasi
