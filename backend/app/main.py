"""
Main FastAPI Application Entrypoint.

Initializes application lifecycle, middleware, routers, and health checks.
"""

from contextlib import asynccontextmanager
from datetime import datetime, timezone
import logging
from typing import AsyncGenerator
from fastapi import FastAPI, status
from fastapi.middleware.cors import CORSMiddleware
from app.auth.router import router as auth_router
from app.chatbot.router import router as chat_router
from app.core.config import settings
from app.courses.router import router as courses_router
from app.database.connection import init_supabase
from app.faculty.router import router as faculty_router
from app.learning_paths.router import router as learning_paths_router
from app.quizzes.router import router as quizzes_router
from app.reports.router import router as reports_router
from app.students.router import router as students_router
from app.study_sessions.router import router as study_sessions_router

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("learning_path_backend")


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Application lifespan manager to handle startup and shutdown events."""
    logger.info("Initializing AI Learning Path Generator backend...")
    
    # 1. Initialize Supabase AsyncClient singleton
    try:
        await init_supabase()
        logger.info("Supabase database connection initialized successfully.")
    except Exception as exc:
        logger.error("Supabase initialization encountered an error: %s", exc)

    # 2. Check Gemini API key configuration
    if settings.GEMINI_API_KEY:
        logger.info("Google GenAI SDK configured with model: %s", settings.GEMINI_MODEL)
    else:
        logger.warning("GEMINI_API_KEY is not set. AI generation will operate in fallback mode.")

    # 3. Check Brevo configuration
    if settings.BREVO_API_KEY:
        logger.info("Brevo email dispatch service is active.")
    else:
        logger.warning("BREVO_API_KEY is not set. OTP codes will be printed to terminal logs.")

    logger.info("Application startup sequence completed.")
    yield
    logger.info("Application shutting down.")


app = FastAPI(
    title="AI-Powered Personalized Learning Path Generator",
    description=(
        "Next-generation personalized education and adaptive learning path platform "
        "powered by Google Gemini GenAI, Supabase, and Brevo."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# CORS Middleware
origins = settings.CORS_ORIGINS if settings.CORS_ORIGINS else ["*"]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register All API Routers
app.include_router(auth_router)
app.include_router(students_router)
app.include_router(faculty_router)
app.include_router(courses_router)
app.include_router(learning_paths_router)
app.include_router(quizzes_router)
app.include_router(reports_router)
app.include_router(chat_router)
app.include_router(study_sessions_router)


@app.get(
    "/",
    status_code=status.HTTP_200_OK,
    tags=["Root"],
    summary="Application root information",
)
async def root() -> dict[str, str]:
    """Welcome endpoint providing documentation links and platform metadata."""
    return {
        "service": "AI-Powered Personalized Learning Path Generator API",
        "version": "1.0.0",
        "status": "online",
        "documentation": "/docs",
        "institutional_domain": settings.STUDENT_INSTITUTIONAL_DOMAIN,
    }


@app.get(
    "/health",
    status_code=status.HTTP_200_OK,
    tags=["Health"],
    summary="Service health and environment diagnostics",
)
async def health_check() -> dict[str, str]:
    """Health check endpoint for container monitors, Render, and uptime probes."""
    return {
        "status": "healthy",
        "environment": settings.ENVIRONMENT,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "gemini_model": settings.GEMINI_MODEL,
    }
