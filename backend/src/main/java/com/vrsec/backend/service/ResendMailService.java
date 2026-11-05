package com.vrsec.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ResendMailService {

    private static final String GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxnM2WQwqcesxVNHtcu-7Oa39USHuv_cPFDgxVfZz943uTdQCT5XtKNpDqh4gWXynA6/exec";

    public void sendOtpEmail(String toEmail, String otpCode) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("email", toEmail);
        body.put("otp", otpCode);

        HttpEntity<?> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_SCRIPT_URL, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection()) {
            System.out.println("OTP email dispatched via Google Apps Script to: " + toEmail);
        } else {
            throw new RuntimeException("Failed to send OTP via Google Script: " + response.getStatusCode());
        }
    }
}