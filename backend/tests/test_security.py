import pytest
from app.core.security import (
    generate_otp,
    hash_otp,
    verify_otp,
    create_access_token,
    create_refresh_token,
    decode_token,
    verify_faculty_email,
)

def test_generate_otp():
    otp = generate_otp()
    assert len(otp) == 6
    assert otp.isdigit()

def test_hash_and_verify_otp():
    otp = generate_otp()
    otp_hash = hash_otp(otp)
    assert otp_hash != otp
    assert verify_otp(otp, otp_hash) is True
    assert verify_otp("000000" if otp != "000000" else "111111", otp_hash) is False

def test_jwt_tokens():
    payload = {"sub": "user-uuid-123", "role": "student"}
    access_token = create_access_token(payload)
    assert isinstance(access_token, str)

    decoded = decode_token(access_token)
    assert decoded["sub"] == "user-uuid-123"
    assert decoded["role"] == "student"
    assert "exp" in decoded

def test_jwt_refresh_token():
    payload = {"sub": "user-uuid-456", "role": "faculty"}
    refresh_token = create_refresh_token(payload)
    assert isinstance(refresh_token, str)

    decoded = decode_token(refresh_token)
    assert decoded["sub"] == "user-uuid-456"
    assert decoded["role"] == "faculty"

def test_faculty_email_verification():
    # Authorized domain
    assert verify_faculty_email("hod_cse@siddhartha.ac.in") is True
    assert verify_faculty_email("dean.academics@siddhartha.ac.in") is True
    
    # Authorized special email
    assert verify_faculty_email("vivekvaranasi99@gmail.com") is True
    assert verify_faculty_email("VIVEKVARANASI99@GMAIL.COM") is True

    # Unauthorized emails
    assert verify_faculty_email("otherperson@gmail.com") is False
    assert verify_faculty_email("student@vrsec.ac.in") is False
    assert verify_faculty_email("admin@yahoo.com") is False
