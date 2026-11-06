"""
Brevo Transactional Email Live Verification Script.

Tests API key authentication, sender validation, and dispatches an actual
test 6-digit verification email.
"""

import asyncio
import os
import sys
from dotenv import load_dotenv

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
load_dotenv()

async def verify_brevo(target_email: str = "vivekvaranasi99@gmail.com"):
    api_key = os.getenv("BREVO_API_KEY")
    sender_email = os.getenv("BREVO_SENDER_EMAIL")
    sender_name = os.getenv("BREVO_SENDER_NAME", "AI Learning Path Generator")

    if not api_key or "your-brevo-api-key" in api_key:
        print("[FAIL] Missing or placeholder BREVO_API_KEY.")
        print("Please configure real credentials in backend/.env.")
        sys.exit(1)

    if not sender_email:
        print("[FAIL] Missing BREVO_SENDER_EMAIL.")
        sys.exit(1)

    print(f"[*] Testing Brevo dispatch from verified sender: {sender_email}")
    print(f"[*] Target recipient: {target_email}")

    from app.core.email import send_otp_email
    success = await send_otp_email(
        to_email=target_email,
        otp="948123",
        purpose="Live System Verification"
    )

    if success:
        print("\n=======================================================")
        print(f"[PASSED] Real test OTP email dispatched to {target_email}!")
        print("Please check your email inbox/spam folder to verify receipt.")
        print("=======================================================")
    else:
        print("\n[FAIL] Brevo email delivery failed. Verify API key and sender authorization.")
        sys.exit(1)

if __name__ == "__main__":
    recipient = sys.argv[1] if len(sys.argv) > 1 else "vivekvaranasi99@gmail.com"
    asyncio.run(verify_brevo(recipient))
