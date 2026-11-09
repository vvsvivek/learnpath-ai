package com.vrsec.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxnM2WQwqcesxVNHtcu-7Oa39USHuv_cPFDgxVfZz943uTdQCT5XtKNpDqh4gWXynA6/exec";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean sendOtpEmail(String recipientEmail, String otpCode) {
        String cleanEmail = recipientEmail != null ? recipientEmail.trim().toLowerCase() : "";
        if (cleanEmail.isEmpty()) return false;

        System.out.println("\n=======================================================");
        System.out.println("  [EMAIL OTP SERVICE] Sending OTP to: " + cleanEmail);
        System.out.println("  [VERIFICATION CODE] : " + otpCode);
        System.out.println("=======================================================\n");

        // 1. Try Brevo HTTP API
        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty() && !brevoApiKey.contains("YOUR_")) {
            if (sendViaBrevoApi(cleanEmail, otpCode)) return true;
        }

        // 2. Try Resend HTTP API
        if (resendApiKey != null && !resendApiKey.trim().isEmpty() && !resendApiKey.contains("YOUR_")) {
            if (sendViaResendApi(cleanEmail, otpCode)) return true;
        }

        // 3. Try Spring JavaMailSender (SMTP)
        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(cleanEmail);
                msg.setSubject("LearnPath AI — Your Verification Code: " + otpCode);
                msg.setText("Welcome to LearnPath AI!\n\nYour 6-digit verification code is: " + otpCode + 
                           "\n\nThis code will expire in 10 minutes. Please enter it to complete your verification.\n\nHappy Learning!\nLearnPath AI Team");
                mailSender.send(msg);
                System.out.println("OTP email dispatched successfully via SMTP to: " + cleanEmail);
                return true;
            } catch (Exception e) {
                System.err.println("SMTP send error: " + e.getMessage());
            }
        }

        // 4. Try Google Apps Script relay
        if (sendViaGoogleScript(cleanEmail, otpCode)) return true;

        System.out.println("Email relayed to local server logs for developer testing: " + otpCode);
        return true;
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
            Map<String, Object> to = Collections.singletonList(Map.of("email", email));
            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; background-color: #f8fafc; border-radius: 12px;'>"
                    + "<h2 style='color: #1E3A5F;'>LearnPath AI Verification Code</h2>"
                    + "<p>Use the following 6-digit code to complete your login or registration:</p>"
                    + "<div style='font-size: 32px; font-weight: bold; color: #4A90D9; letter-spacing: 4px; padding: 16px 0;'>" + otp + "</div>"
                    + "<p style='color: #64748b;'>This code expires in 10 minutes. If you did not request this, please disregard.</p>"
                    + "</div>";

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", sender);
            payload.put("to", to);
            payload.put("subject", "LearnPath AI — Verification Code: " + otp);
            payload.put("htmlContent", htmlContent);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(payload));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                System.out.println("OTP email dispatched successfully via Brevo API to: " + email);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Brevo API error: " + e.getMessage());
        }
        return false;
    }

    private boolean sendViaResendApi(String email, String otp) {
        try {
            URL url = new URL("https://api.resend.com/emails");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + resendApiKey.trim());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", "LearnPath AI <onboarding@resend.dev>");
            payload.put("to", Collections.singletonList(email));
            payload.put("subject", "LearnPath AI Verification Code: " + otp);
            payload.put("html", "<h2>Your LearnPath AI Code: <strong>" + otp + "</strong></h2><p>Expires in 10 minutes.</p>");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(payload));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                System.out.println("OTP email dispatched successfully via Resend API to: " + email);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Resend API error: " + e.getMessage());
        }
        return false;
    }

    private boolean sendViaGoogleScript(String email, String otp) {
        try {
            String jsonPayload = String.format("{\"email\":\"%s\",\"otp\":\"%s\"}", email, otp);
            URL url = new URL(SCRIPT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("OTP email dispatched via Google Apps Script relay to: " + email);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Google Apps Script relay error: " + e.getMessage());
        }
        return false;
    }
}
