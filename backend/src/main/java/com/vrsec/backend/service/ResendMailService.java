package com.vrsec.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResendMailService {

    @Autowired
    private EmailService emailService;

    public void sendOtpEmail(String recipientEmail, String otpCode) {
        emailService.sendOtpEmail(recipientEmail, otpCode);
    }
}
