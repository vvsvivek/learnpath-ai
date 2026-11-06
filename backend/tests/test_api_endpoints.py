import pytest
from unittest.mock import AsyncMock, MagicMock
from fastapi.testclient import TestClient
from app.main import app
from app.core.dependencies import get_supabase
from app.core.rate_limiter import limiter

# Mock Supabase client
mock_supabase = MagicMock()
mock_table = MagicMock()
mock_supabase.table.return_value = mock_table
mock_table.update.return_value = mock_table
mock_table.eq.return_value = mock_table
mock_table.insert.return_value = mock_table
mock_table.select.return_value = mock_table
mock_table.execute = AsyncMock(return_value=MagicMock(data=[]))

app.dependency_overrides[get_supabase] = lambda: mock_supabase

client = TestClient(app)

@pytest.fixture(autouse=True)
def reset_rate_limiter():
    """Clear in-memory rate limiter store before each test run."""
    limiter._store.clear()

def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "online"
    assert "version" in data
    assert data["institutional_domain"] == "@vrsec.ac.in"

def test_health_check_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "timestamp" in data
    assert "gemini_model" in data

def test_auth_request_otp_invalid_email_format():
    response = client.post("/auth/request-otp", json={"email": "invalid-email-format", "role": "student"})
    assert response.status_code == 422

def test_auth_request_otp_unauthorized_personal_email():
    response = client.post("/auth/request-otp", json={"email": "unauthorized@gmail.com", "role": "student"})
    assert response.status_code == 403
    assert "restricted" in response.json()["detail"].lower()

def test_auth_request_otp_authorized_student():
    response = client.post("/auth/request-otp", json={"email": "student21@vrsec.ac.in", "role": "student"})
    assert response.status_code == 200
    assert response.json()["success"] is True

def test_auth_request_otp_authorized_faculty_siddhartha():
    response = client.post("/auth/request-otp", json={"email": "hod_cse@siddhartha.ac.in", "role": "faculty"})
    assert response.status_code == 200
    assert response.json()["success"] is True

def test_auth_request_otp_authorized_faculty_special():
    response = client.post("/auth/request-otp", json={"email": "vivekvaranasi99@gmail.com", "role": "faculty"})
    assert response.status_code == 200
    assert response.json()["success"] is True

def test_auth_request_otp_role_mismatch_prevented():
    # Attempting to register @vrsec.ac.in as faculty must be blocked
    response = client.post("/auth/request-otp", json={"email": "another_student@vrsec.ac.in", "role": "faculty"})
    assert response.status_code == 403
    assert "corresponds to student role" in response.json()["detail"].lower()
