package com.vrsec.backend.controller;

import java.util.Optional;
import java.util.Random; // Used to generate a 6-digit OTP

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.UserRepository;
import com.vrsec.backend.service.ResendMailService; // Import your new service

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") 
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResendMailService resendMailService; // Inject the Resend mail service

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
        
        // Optional: Generate a 6-digit OTP code to send upon registration
        String generatedOtp = String.format("%06d", new Random().nextInt(999999));
        
        try {
            // Send the OTP via Resend HTTP API (bypasses Render's blocked SMTP ports)
            resendMailService.sendOtpEmail(registerRequest.getEmail(), generatedOtp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send verification email: " + e.getMessage());
        }

        User savedUser = userRepository.save(registerRequest);
        return ResponseEntity.ok(savedUser);
    }
}