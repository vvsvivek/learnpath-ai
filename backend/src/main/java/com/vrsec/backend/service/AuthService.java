package com.vrsec.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrsec.backend.exception.EmailDeliveryException;
import com.vrsec.backend.model.EmailOtp;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.EmailOtpRepository;
import com.vrsec.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(noRollbackFor = {IllegalArgumentException.class, EmailDeliveryException.class})
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtService jwtService;

    private Clock clock = Clock.systemDefaultZone();

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public Clock getClock() {
        return clock;
    }

    private final SecureRandom secureRandom = new SecureRandom();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Institutional Email & Role Enforcement:
     * - Faculty: Any email ending in @siddhartha.ac.in or vivekvaranasi99@gmail.com
     * - Student: Strictly emails ending in @vrsec.ac.in
     * - Any other domain: Rejected as unauthorized institutional domain
     */
    public boolean isAuthorizedInstitutionalEmail(String email) {
        if (email == null) return false;
        String clean = email.trim().toLowerCase();
        boolean isFaculty = clean.endsWith("@siddhartha.ac.in") || clean.equalsIgnoreCase("vivekvaranasi99@gmail.com");
        boolean isStudent = clean.endsWith("@vrsec.ac.in");
        return isFaculty || isStudent;
    }

    public String determineRole(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required.");
        }
        String clean = email.trim().toLowerCase();
        if (clean.endsWith("@siddhartha.ac.in") || clean.equalsIgnoreCase("vivekvaranasi99@gmail.com")) {
            return "FACULTY";
        }
        if (clean.endsWith("@vrsec.ac.in")) {
            return "STUDENT";
        }
        throw new IllegalArgumentException("Unauthorized email domain. Registration is restricted to @vrsec.ac.in for students and @siddhartha.ac.in for faculty.");
    }

    public Map<String, Object> sendOtp(String rawEmail) {
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String cleanEmail = rawEmail.trim().toLowerCase();

        // Enforce institutional domain restriction
        if (!isAuthorizedInstitutionalEmail(cleanEmail)) {
            if (userRepository.findByEmail(cleanEmail).isEmpty()) {
                throw new IllegalArgumentException("Invalid institutional email. Students must register with @vrsec.ac.in and faculty with @siddhartha.ac.in.");
            }
        }

        // P2: 60-second server-side resend cooldown with concurrency protection
        // Canonical string lock ensures simultaneous requests for the same email are serialized
        synchronized (cleanEmail.intern()) {
            Optional<EmailOtp> latestOtpOpt = otpRepository.findTopByEmailOrderByCreatedAtDesc(cleanEmail);
            if (latestOtpOpt.isPresent()) {
                EmailOtp latest = latestOtpOpt.get();
                if (latest.getCreatedAt() != null) {
                    long secondsElapsed = Duration.between(latest.getCreatedAt(), LocalDateTime.now(clock)).getSeconds();
                    if (secondsElapsed >= 0 && secondsElapsed < 60) {
                        long remaining = 60 - secondsElapsed;
                        throw new IllegalArgumentException("Please wait " + remaining + " seconds before requesting another verification code.");
                    }
                }
            }

            // Generate 6-digit cryptographic OTP
            String otpCode = String.format("%06d", secureRandom.nextInt(1000000));

            // Invalidate older unused OTPs for this email
            List<EmailOtp> oldOtps = otpRepository.findByEmailOrderByCreatedAtDesc(cleanEmail);
            for (EmailOtp old : oldOtps) {
                if (!old.isUsed()) {
                    old.setUsed(true);
                    otpRepository.save(old);
                }
            }

            // Save new OTP in database with 10-minute expiry (deterministic timestamp via Clock)
            LocalDateTime now = LocalDateTime.now(clock);
            EmailOtp otpEntity = new EmailOtp();
            otpEntity.setEmail(cleanEmail);
            otpEntity.setOtpCode(otpCode);
            otpEntity.setPurpose("VERIFICATION");
            otpEntity.setUsed(false);
            otpEntity.setAttempts(0);
            otpEntity.setCreatedAt(now);
            otpEntity.setExpiresAt(now.plusMinutes(10));
            otpRepository.save(otpEntity);

            // Dispatch email via Brevo
            boolean dispatched = emailService.sendOtpEmail(cleanEmail, otpCode);

            // P1: Handle Brevo dispatch failure reliably without rolling back invalidation
            if (!dispatched) {
                otpEntity.setUsed(true);
                otpRepository.save(otpEntity);
                throw new EmailDeliveryException("Email delivery service failed to dispatch verification code. Please check your address or try again shortly.");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification code sent to " + cleanEmail);
            response.put("email", cleanEmail);
            response.put("dispatched", true);
            return response;
        }
    }

    public Map<String, Object> verifyOtp(String rawEmail, String otpCode, String providedName, String optionalPassword) {
        if (rawEmail == null || rawEmail.trim().isEmpty() || otpCode == null || otpCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Email and OTP code are required.");
        }
        String cleanEmail = rawEmail.trim().toLowerCase();
        String cleanOtp = otpCode.trim();

        // Retrieve latest active OTP from database
        Optional<EmailOtp> otpOpt = otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(cleanEmail);
        if (otpOpt.isEmpty()) {
            throw new IllegalArgumentException("No active verification code found for this email. Please request a new code.");
        }

        EmailOtp otpEntity = otpOpt.get();
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isAfter(otpEntity.getExpiresAt())) {
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }

        if (otpEntity.getAttempts() >= 5) {
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
            throw new IllegalArgumentException("Too many incorrect attempts. Please request a new code.");
        }

        boolean isMatchingCode = otpEntity.getOtpCode().equals(cleanOtp);

        if (!isMatchingCode) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            otpRepository.save(otpEntity);
            throw new IllegalArgumentException("Invalid verification code. Please check and try again.");
        }

        // OTP is verified successfully!
        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);

        // Find or create user
        String assignedRole = determineRole(cleanEmail);
        User user = userRepository.findByEmail(cleanEmail).orElse(null);

        if (user == null) {
            user = new User();
            user.setEmail(cleanEmail);
            user.setUsername(providedName != null && !providedName.trim().isEmpty() 
                ? providedName.trim() 
                : cleanEmail.split("@")[0]);
            user.setRole(assignedRole);
            user.setIsEmailVerified(true);
            user.setAuthProvider("EMAIL");
            user.setCgpa(0.0);
            if (optionalPassword != null && !optionalPassword.trim().isEmpty()) {
                user.setPassword(hashPassword(optionalPassword.trim()));
            }
            // P3: Do NOT hardcode CSE, 2nd Year, 1st Sem for new students.
            // Leave branch, academicYear, and semester null so new students complete onboarding on first login.
            user = userRepository.save(user);
        } else {
            // Re-affirm backend calculated role
            user.setRole(assignedRole);
            user.setIsEmailVerified(true);
            if (optionalPassword != null && !optionalPassword.trim().isEmpty()) {
                user.setPassword(hashPassword(optionalPassword.trim()));
            }
            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", sanitizeUser(user));
        result.put("role", user.getRole());
        return result;
    }

    public Map<String, Object> authenticateWithGoogle(String idTokenString) {
        try {
            if (idTokenString == null || idTokenString.trim().isEmpty()) {
                throw new IllegalArgumentException("Google ID token is required.");
            }

            // Verify Google token via Google OAuth tokeninfo endpoint
            String verifyUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idTokenString.trim();
            Map<String, Object> googlePayload = restTemplate.getForObject(verifyUrl, Map.class);

            if (googlePayload == null || !googlePayload.containsKey("email")) {
                throw new IllegalArgumentException("Invalid Google token or unable to retrieve email.");
            }

            String email = ((String) googlePayload.get("email")).toLowerCase().trim();
            String name = (String) googlePayload.getOrDefault("name", email.split("@")[0]);

            String assignedRole = determineRole(email);
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setUsername(name);
                user.setRole(assignedRole);
                user.setIsEmailVerified(true);
                user.setAuthProvider("GOOGLE");
                user.setCgpa(0.0);
                user = userRepository.save(user);
            } else {
                user.setRole(assignedRole);
                user.setIsEmailVerified(true);
                user.setAuthProvider("GOOGLE");
                user = userRepository.save(user);
            }

            String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", sanitizeUser(user));
            result.put("role", user.getRole());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> passwordLogin(String rawEmail, String password) {
        if (rawEmail == null || password == null) {
            throw new IllegalArgumentException("Email and password are required.");
        }
        String cleanEmail = rawEmail.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        String hashed = hashPassword(password);
        if (user.getPassword() != null && (user.getPassword().equals(hashed) || user.getPassword().equals(password))) {
            String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", sanitizeUser(user));
            result.put("role", user.getRole());
            return result;
        }
        throw new IllegalArgumentException("Invalid email or password.");
    }

    public Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("username", user.getUsername());
        map.put("role", user.getRole());
        map.put("branch", user.getBranch());
        map.put("academicYear", user.getAcademicYear());
        map.put("semester", user.getSemester());
        map.put("cgpa", user.getCgpa());
        map.put("isEmailVerified", user.getIsEmailVerified());
        map.put("authProvider", user.getAuthProvider());
        return map;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }
}
