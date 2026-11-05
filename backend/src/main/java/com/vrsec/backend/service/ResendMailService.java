package com.vrsec.backend.service;

import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class ResendMailService {

    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxnM2WQwqcesxVNHtcu-7Oa39USHuv_cPFDgxVfZz943uTdQCT5XtKNpDqh4gWXynA6/exec";

    public void sendOtpEmail(String recipientEmail, String otpCode) {
        try {
            String jsonPayload = String.format("{\"email\":\"%s\",\"otp\":\"%s\"}", recipientEmail, otpCode);
            
            URL url = new URL(SCRIPT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("OTP email dispatched via Google Apps Script to: " + recipientEmail);
            } else {
                System.err.println("Failed to send email. Response code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}