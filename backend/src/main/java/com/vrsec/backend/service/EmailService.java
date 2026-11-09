package com.vrsec.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production Email Service — BREVO ONLY.
 * All third-party fallbacks (Resend, SMTP, Google Apps Script) are disabled.
 * Security: OTP codes, secrets, and credentials are NEVER printed in logs.
 */
@Service
public class EmailService {

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean sendOtpEmail(String recipientEmail, String otpCode) {
        String cleanEmail = recipientEmail != null ? recipientEmail.trim().toLowerCase() : "";
        if (cleanEmail.isEmpty()) return false;

        if (brevoApiKey == null || brevoApiKey.trim().isEmpty() || brevoApiKey.contains("YOUR_")) {
            System.err.println("[EMAIL SERVICE] BREVO_API_KEY is not configured. Please set BREVO_API_KEY in Render environment variables.");
            return false;
        }

        // Brevo HTTP API (Exclusive Provider)
        return sendViaBrevoApi(cleanEmail, otpCode);
    }

    private boolean sendViaBrevoApi(String email, String otp) {
        try {
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", brevoApiKey.trim());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            Map<String, Object> sender = Map.of("name", "LearnPath AI", "email", "noreply@learnpath.ai");
            List<Map<String, Object>> to = Collections.singletonList(Map.of("email", email));
            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 24px; background-color: #f8fafc; border-radius: 12px; max-width: 500px; margin: auto;'>"
                    + "<h2 style='color: #1E3A5F; margin-bottom: 8px;'>LearnPath AI Verification Code</h2>"
                    + "<p style='color: #475569; font-size: 15px;'>Use the following 6-digit code to complete your verification:</p>"
                    + "<div style='font-size: 32px; font-weight: bold; color: #4A90D9; letter-spacing: 6px; padding: 20px 0; text-align: center; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0;'>" + otp + "</div>"
                    + "<p style='color: #94a3b8; font-size: 13px; margin-top: 16px;'>This code expires in 10 minutes. If you did not request this, please disregard this email.</p>"
                    + "</div>";

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", sender);
            payload.put("to", to);
            payload.put("subject", "LearnPath AI — Your Verification Code");
            payload.put("htmlContent", htmlContent);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(payload));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                System.out.println("[EMAIL SERVICE] Verification email successfully dispatched via Brevo API to recipient.");
                return true;
            } else {
                System.err.println("[EMAIL SERVICE] Brevo API responded with HTTP status: " + code);
            }
        } catch (Exception e) {
            System.err.println("[EMAIL SERVICE] Failed to dispatch email via Brevo API: " + e.getMessage());
        }
        return false;
    }
}
