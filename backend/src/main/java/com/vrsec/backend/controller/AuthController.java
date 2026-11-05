package com.vrsec.backend.controller;

import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.UserRepository;
import com.vrsec.backend.service.ResendMailService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") 
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResendMailService resendMailService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> user = userRepository.findByEmail(loginRequest.getEmail());
        
        if (user.isPresent() && user.get().getPassword().equals(loginRequest.getPassword())) {
            return ResponseEntity.ok(user.get()); 
        }
        return ResponseEntity.status(401).body("Invalid email or password");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        
        // Generate a 6-digit OTP code to send via Resend
        String generatedOtp = String.format("%06d", new Random().nextInt(999999));
        
        try {
            resendMailService.sendOtpEmail(registerRequest.getEmail(), generatedOtp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send verification email: " + e.getMessage());
        }

        User savedUser = userRepository.save(registerRequest);
        return ResponseEntity.ok(savedUser);
    }
    
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody User otpRequest) {
        String generatedOtp = String.format("%06d", new Random().nextInt(999999));
        try {
            resendMailService.sendOtpEmail(otpRequest.getEmail(), generatedOtp);
            return ResponseEntity.ok("OTP sent successfully via Resend");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send OTP: " + e.getMessage());
        }
    }
}