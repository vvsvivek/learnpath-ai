"""
Email Notification Service via Brevo API.
"""

import logging
from typing import Any
import httpx
from app.core.config import settings

logger = logging.getLogger(__name__)


def generate_otp_email_html(otp: str, purpose: str, expire_minutes: int) -> str:
    """Generate a modern, beautiful HTML email template for OTP verification."""
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Your Verification Code</title>
</head>
<body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7fa; color: #1e293b;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #f4f7fa; padding: 40px 10px;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" style="max-width: 540px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #e2e8f0;">
          <!-- Header -->
          <tr>
            <td style="background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%); padding: 32px 30px; text-align: center;">
              <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">
                AI Learning Path Generator
              </h1>
              <p style="margin: 8px 0 0; color: #e0e7ff; font-size: 14px;">
                Personalized AI-Powered Educational Platform
              </p>
            </td>
          </tr>
          
          <!-- Content Body -->
          <tr>
            <td style="padding: 36px 32px 24px;">
              <h2 style="margin: 0 0 12px; color: #0f172a; font-size: 18px; font-weight: 600;">
                {purpose} Verification Code
              </h2>
              <p style="margin: 0 0 24px; color: #475569; font-size: 15px; line-height: 1.6;">
                Use the one-time passcode below to complete your authentication. For security reasons, this code will expire in <strong>{expire_minutes} minutes</strong>.
              </p>

              <!-- OTP Box -->
              <div style="background-color: #f8fafc; border: 2px dashed #cbd5e1; border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 24px;">
                <span style="font-family: 'Courier New', Courier, monospace; font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #4f46e5; display: inline-block;">
                  {otp}
                </span>
              </div>

              <div style="background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 4px; margin-bottom: 24px;">
                <p style="margin: 0; color: #92400e; font-size: 13px; line-height: 1.5;">
                  <strong>Security Reminder:</strong> Never share this code with anyone. Our faculty and support staff will never request your one-time code.
                </p>
              </div>

              <p style="margin: 0; color: #64748b; font-size: 13px; line-height: 1.5;">
                If you did not request this verification code, please ignore this email or reach out to your administrator immediately.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background-color: #f8fafc; padding: 20px 32px; border-top: 1px solid #e2e8f0; text-align: center;">
              <p style="margin: 0; color: #94a3b8; font-size: 12px;">
                &copy; 2026 AI Personalized Learning Path Generator &bull; Siddhartha Academy
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
"""


async def send_otp_email(to_email: str, otp: str, purpose: str = "Sign In") -> bool:
    """Send an OTP email to the user using Brevo API.

    Args:
        to_email: Recipient's email address.
        otp: The 6-digit plain OTP.
        purpose: Purpose label (e.g. "Student Login", "Faculty Access").

    Returns:
        True if the email was dispatched successfully, False otherwise.
    """
    if not settings.BREVO_API_KEY:
        logger.warning(
            "BREVO_API_KEY is not configured! OTP for %s is [%s] (development mode).",
            to_email,
            otp,
        )
        return True

    subject = f"{otp} is your verification code for AI Learning Platform"
    html_content = generate_otp_email_html(
        otp=otp,
        purpose=purpose,
        expire_minutes=settings.OTP_EXPIRE_MINUTES,
    )

    # Attempt sending via brevo-python SDK first
    try:
        from brevo import Brevo
        from brevo.transactional_emails import (
            SendTransacEmailRequestSender,
            SendTransacEmailRequestToItem,
        )

        client = Brevo(api_key=settings.BREVO_API_KEY)
        client.transactional_emails.send_transac_email(
            subject=subject,
            html_content=html_content,
            sender=SendTransacEmailRequestSender(
                name=settings.BREVO_SENDER_NAME,
                email=settings.BREVO_SENDER_EMAIL,
            ),
            to=[
                SendTransacEmailRequestToItem(
                    email=to_email,
                )
            ],
        )
        logger.info("OTP email successfully sent to %s via Brevo SDK", to_email)
        return True
    except Exception as sdk_err:
        logger.warning("Brevo SDK send failed: %s. Falling back to Brevo HTTP API...", sdk_err)

    # Robust Fallback: direct HTTP call to Brevo v3 transactional email API
    try:
        async with httpx.AsyncClient(timeout=10.0) as http_client:
            payload: dict[str, Any] = {
                "sender": {
                    "name": settings.BREVO_SENDER_NAME,
                    "email": settings.BREVO_SENDER_EMAIL,
                },
                "to": [{"email": to_email}],
                "subject": subject,
                "htmlContent": html_content,
            }
            headers = {
                "accept": "application/json",
                "api-key": settings.BREVO_API_KEY,
                "content-type": "application/json",
            }
            response = await http_client.post(
                "https://api.brevo.com/v3/smtp/email",
                json=payload,
                headers=headers,
            )
            if response.status_code in (200, 201, 202):
                logger.info("OTP email successfully sent to %s via Brevo HTTP API", to_email)
                return True
            else:
                logger.error(
                    "Brevo HTTP API returned status %s: %s",
                    response.status_code,
                    response.text,
                )
                return False
    except Exception as exc:
        logger.exception("Failed to send OTP email to %s: %s", to_email, exc)
        return False
