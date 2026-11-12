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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Faculty Email Eligibility:
     * - Any valid email ending in @siddhartha.ac.in
     * - Explicitly allowlisted email: vivekvaranasi99@gmail.com
     * - Any other personal or student email: rejected for faculty
     */
    public boolean isEligibleFacultyEmail(String email) {
        if (email == null) return false;
        String clean = email.trim().toLowerCase();
        return clean.endsWith("@siddhartha.ac.in") || clean.equalsIgnoreCase("vivekvaranasi99@gmail.com");
    }

    public boolean isAuthorizedInstitutionalEmail(String email) {
        if (email == null) return false;
        String clean = email.trim().toLowerCase();
        return isEligibleFacultyEmail(clean) || clean.endsWith("@vrsec.ac.in");
    }

    /**
     * Explicit server-controlled registry of designated HOD and course approver email addresses.
     * Institutional domain membership or username text alone NEVER confers approval authority.
     * Only addresses explicitly confirmed as authorized course approvers are enrolled.
     */
    private static final Set<String> DESIGNATED_COURSE_APPROVERS = ConcurrentHashMap.newKeySet();

    static {
        // Only explicitly confirmed departmental HOD / course approver addresses
        DESIGNATED_COURSE_APPROVERS.add("hod.cse@siddhartha.ac.in");

        // Optional server environment-configured approver addresses
        String envApprovers = System.getenv("COURSE_APPROVER_EMAILS");
        if (envApprovers != null && !envApprovers.trim().isEmpty()) {
            for (String email : envApprovers.split(",")) {
                String clean = email.trim().toLowerCase();
                if (!clean.isEmpty()) {
                    DESIGNATED_COURSE_APPROVERS.add(clean);
                }
            }
        }
    }

    public static boolean isDesignatedApproverEmail(String email) {
        if (email == null) return false;
        return DESIGNATED_COURSE_APPROVERS.contains(email.trim().toLowerCase());
    }

    public static void addDesignatedApprover(String email) {
        if (email != null && !email.trim().isEmpty()) {
            DESIGNATED_COURSE_APPROVERS.add(email.trim().toLowerCase());
        }
    }

    public static void removeDesignatedApprover(String email) {
        if (email != null && !email.trim().isEmpty()) {
            DESIGNATED_COURSE_APPROVERS.remove(email.trim().toLowerCase());
        }
    }

    public static Set<String> getDesignatedCourseApprovers() {
        return Collections.unmodifiableSet(DESIGNATED_COURSE_APPROVERS);
    }

    /**
     * Checks if a user is explicitly authorized to approve course catalogue additions.
     * 
     * STRICT SECURITY POLICY:
     * 1. Authorization is NEVER granted based on email prefixes, domain membership alone, or usernames.
     * 2. The approver's email must be derived from a verified server-side JWT.
     * 3. The approver account must exist in the database with email verification confirmed (isEmailVerified = true).
     * 4. Approval is strictly restricted to:
     *    - Users with server-side database role ADMIN
     *    - Users with server-side database role FACULTY who are explicitly listed in DESIGNATED_COURSE_APPROVERS
     * 5. Ordinary faculty, students, unverified accounts, and unlisted accounts are strictly rejected.
     */
    public boolean isAuthorizedCourseApprover(User approver, String verifiedEmail) {
        if (verifiedEmail == null || approver == null) return false;
        String cleanEmail = verifiedEmail.trim().toLowerCase();

        // 1. Server-side email verification check: Unverified accounts can never approve
        if (!Boolean.TRUE.equals(approver.getIsEmailVerified())) {
            return false;
        }

        // 2. Server-side identity match: database record email must match token verified email
        if (approver.getEmail() == null || !approver.getEmail().equalsIgnoreCase(cleanEmail)) {
            return false;
        }

        // 3. System Administrator role from trusted database record
        if ("ADMIN".equalsIgnoreCase(approver.getRole())) {
            return true;
        }

        // 4. Must be a verified FACULTY user in the database
        if (!"FACULTY".equalsIgnoreCase(approver.getRole())) {
            return false;
        }

        // 5. User email must be explicitly listed in the server-controlled designated approver registry
        return isDesignatedApproverEmail(cleanEmail);
    }

    /**
     * Determine role with explicit backend validation:
     * - If user already exists in DB, preserves existing role.
     * - If FACULTY requested: validated against isEligibleFacultyEmail.
     * - If STUDENT requested: students can register with ANY valid email address.
     */
    public String determineRole(String email, String requestedRole) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String clean = email.trim().toLowerCase();
        if (!isValidEmail(clean)) {
            throw new IllegalArgumentException("Invalid email format. Please provide a valid email address.");
        }

        // Preserve existing user role if already registered
        Optional<User> existing = userRepository.findByEmail(clean);
        if (existing.isPresent()) {
            return existing.get().getRole();
        }

        // For new registrations:
        if ("FACULTY".equalsIgnoreCase(requestedRole)) {
            if (isEligibleFacultyEmail(clean)) {
                return "FACULTY";
            }
            throw new IllegalArgumentException("Unauthorized faculty registration. Faculty registration is restricted to @siddhartha.ac.in institutional emails or authorized faculty addresses.");
        }

        // Students can register with any valid email address
        return "STUDENT";
    }

    public String determineRole(String email) {
        return determineRole(email, null);
    }

    public Map<String, Object> sendOtp(String rawEmail) {
        return sendOtp(rawEmail, null, null);
    }

    public Map<String, Object> sendOtp(String rawEmail, String requestedRole, String mode) {
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String cleanEmail = rawEmail.trim().toLowerCase();

        if (!isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException("Invalid email format. Please provide a valid email address.");
        }

        // Registration Eligibility & Duplicate Check
        if ("REGISTER".equalsIgnoreCase(mode)) {
            if (userRepository.findByEmail(cleanEmail).isPresent()) {
                throw new IllegalArgumentException("An account with this email already exists. Please sign in instead.");
            }
            if ("FACULTY".equalsIgnoreCase(requestedRole) && !isEligibleFacultyEmail(cleanEmail)) {
                throw new IllegalArgumentException("Unauthorized faculty registration. Faculty registration is restricted to @siddhartha.ac.in institutional emails or authorized faculty addresses.");
            }
        }

        // Sign In / Login Cross-Role Validation
        if ("LOGIN".equalsIgnoreCase(mode) || "SIGN_IN".equalsIgnoreCase(mode)) {
            Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if ("FACULTY".equalsIgnoreCase(requestedRole) && !"FACULTY".equalsIgnoreCase(user.getRole())) {
                    throw new IllegalArgumentException("This account is registered as a Student. Please use the Student Portal.");
                }
                if ("STUDENT".equalsIgnoreCase(requestedRole) && "FACULTY".equalsIgnoreCase(user.getRole())) {
                    throw new IllegalArgumentException("This account is registered as Faculty. Please use the Faculty Portal.");
                }
            } else {
                if ("FACULTY".equalsIgnoreCase(requestedRole) && !isEligibleFacultyEmail(cleanEmail)) {
                    throw new IllegalArgumentException("No faculty account found with this email. Faculty access requires an authorized email.");
                }
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
        return verifyOtp(rawEmail, otpCode, providedName, optionalPassword, null, null);
    }

    public Map<String, Object> verifyOtp(String rawEmail, String otpCode, String providedName, String optionalPassword, String requestedRole, String mode) {
        if (rawEmail == null || rawEmail.trim().isEmpty() || otpCode == null || otpCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Email and OTP code are required.");
        }
        String cleanEmail = rawEmail.trim().toLowerCase();
        String cleanOtp = otpCode.trim();

        if (!isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException("Invalid email format. Please provide a valid email address.");
        }

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

        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);

        // Enforce duplicate registration rejection
        if ("REGISTER".equalsIgnoreCase(mode) && userOpt.isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists. Please sign in instead.");
        }

        User user;
        if (userOpt.isEmpty()) {
            // New user registration
            String assignedRole = determineRole(cleanEmail, requestedRole);
            user = new User();
            user.setEmail(cleanEmail);
            user.setUsername(providedName != null && !providedName.trim().isEmpty() 
                ? providedName.trim() 
                : cleanEmail.split("@")[0]);
            user.setRole(assignedRole);
            user.setIsEmailVerified(true);
            user.setAuthProvider("EMAIL");
            user.setCgpa(null);
            if (optionalPassword != null && !optionalPassword.trim().isEmpty()) {
                user.setPassword(hashPassword(optionalPassword.trim()));
            }
            if ("STUDENT".equalsIgnoreCase(assignedRole)) {
                user.setBranch(null);
                user.setAcademicYear(null);
                user.setSemester(null);
                user.setCgpa(null);
            } else if ("FACULTY".equalsIgnoreCase(assignedRole)) {
                user.setBranch("CSE");
                user.setAcademicYear("Faculty");
                user.setSemester("Faculty");
            }
            user = userRepository.save(user);
        } else {
            // Existing user login: cross-role guard
            user = userOpt.get();
            if ("FACULTY".equalsIgnoreCase(requestedRole) && !"FACULTY".equalsIgnoreCase(user.getRole())) {
                throw new IllegalArgumentException("This account is registered as a Student. Please use the Student Portal.");
            }
            if ("STUDENT".equalsIgnoreCase(requestedRole) && "FACULTY".equalsIgnoreCase(user.getRole())) {
                throw new IllegalArgumentException("This account is registered as Faculty. Please use the Faculty Portal.");
            }
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
                user.setCgpa(null);
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

    public String hashPassword(String password) {
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
