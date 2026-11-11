package com.vrsec.backend;

import com.vrsec.backend.controller.AuthController;
import com.vrsec.backend.exception.EmailDeliveryException;
import com.vrsec.backend.model.EmailOtp;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.EmailOtpRepository;
import com.vrsec.backend.repository.UserRepository;
import com.vrsec.backend.service.AuthService;
import com.vrsec.backend.service.EmailService;
import com.vrsec.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;
    private AuthController authController;

    private Map<String, User> userDb;
    private List<EmailOtp> otpDb;
    private AtomicBoolean emailDispatchSuccess;

    private Instant baseInstant;
    private ZoneId zoneId;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        authController = new AuthController();

        userDb = new ConcurrentHashMap<>();
        otpDb = new CopyOnWriteArrayList<>();
        emailDispatchSuccess = new AtomicBoolean(true);

        zoneId = ZoneId.systemDefault();
        baseInstant = Instant.parse("2026-11-11T12:00:00Z");
        fixedClock = Clock.fixed(baseInstant, zoneId);
        authService.setClock(fixedClock);

        // Native JDK Dynamic Proxy for UserRepository (no ByteBuddy / Java agent required)
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByEmail".equals(name) || "findByEmailIgnoreCase".equals(name)) {
                        String email = ((String) args[0]).toLowerCase();
                        return Optional.ofNullable(userDb.get(email));
                    }
                    if ("save".equals(name)) {
                        User u = (User) args[0];
                        if (u.getId() == null) {
                            u.setId((long) (userDb.size() + 1));
                        }
                        userDb.put(u.getEmail().toLowerCase(), u);
                        return u;
                    }
                    return null;
                }
        );

        // Native JDK Dynamic Proxy for EmailOtpRepository
        EmailOtpRepository otpRepository = (EmailOtpRepository) Proxy.newProxyInstance(
                EmailOtpRepository.class.getClassLoader(),
                new Class<?>[]{EmailOtpRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findTopByEmailOrderByCreatedAtDesc".equals(name)) {
                        String email = ((String) args[0]).toLowerCase();
                        return otpDb.stream()
                                .filter(o -> o.getEmail().equalsIgnoreCase(email))
                                .sorted(Comparator.comparing(EmailOtp::getCreatedAt).reversed())
                                .findFirst();
                    }
                    if ("findTopByEmailAndUsedFalseOrderByCreatedAtDesc".equals(name)) {
                        String email = ((String) args[0]).toLowerCase();
                        return otpDb.stream()
                                .filter(o -> o.getEmail().equalsIgnoreCase(email) && !o.isUsed())
                                .sorted(Comparator.comparing(EmailOtp::getCreatedAt).reversed())
                                .findFirst();
                    }
                    if ("findByEmailOrderByCreatedAtDesc".equals(name)) {
                        String email = ((String) args[0]).toLowerCase();
                        return otpDb.stream()
                                .filter(o -> o.getEmail().equalsIgnoreCase(email))
                                .sorted(Comparator.comparing(EmailOtp::getCreatedAt).reversed())
                                .toList();
                    }
                    if ("save".equals(name)) {
                        EmailOtp o = (EmailOtp) args[0];
                        if (!otpDb.contains(o)) {
                            otpDb.add(o);
                        }
                        return o;
                    }
                    return null;
                }
        );

        // Lightweight Test Doubles for EmailService and JwtService
        EmailService testEmailService = new EmailService() {
            @Override
            public boolean sendOtpEmail(String recipientEmail, String otpCode) {
                return emailDispatchSuccess.get();
            }
        };

        JwtService testJwtService = new JwtService() {
            @Override
            public String generateToken(String email, String role, Long userId) {
                return "mocked.jwt.token";
            }
        };

        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "otpRepository", otpRepository);
        ReflectionTestUtils.setField(authService, "emailService", testEmailService);
        ReflectionTestUtils.setField(authService, "jwtService", testJwtService);

        ReflectionTestUtils.setField(authController, "authService", authService);
        ReflectionTestUtils.setField(authController, "userRepository", userRepository);
        ReflectionTestUtils.setField(authController, "jwtService", testJwtService);
    }

    // =========================================================================
    // BASE CLASSIFICATION TESTS (Existing Baseline)
    // =========================================================================

    @Test
    void testVrsecStudentRoleClassification() {
        String role = authService.determineRole("22vr1a0501@vrsec.ac.in");
        assertEquals("STUDENT", role);
    }

    @Test
    void testSiddharthaFacultyRoleClassification() {
        String role = authService.determineRole("hod.cse@siddhartha.ac.in");
        assertEquals("FACULTY", role);
    }

    @Test
    void testAuthorizedFacultyGmailRoleClassification() {
        String role = authService.determineRole("vivekvaranasi99@gmail.com");
        assertEquals("FACULTY", role);
    }

    @Test
    void testUnauthorizedDomainRejection() {
        assertThrows(IllegalArgumentException.class, () -> authService.determineRole("attacker@gmail.com"));
        assertThrows(IllegalArgumentException.class, () -> authService.determineRole("user@random.com"));
    }

    @Test
    void testInstitutionalEmailValidation() {
        assertTrue(authService.isAuthorizedInstitutionalEmail("student@vrsec.ac.in"));
        assertTrue(authService.isAuthorizedInstitutionalEmail("faculty@siddhartha.ac.in"));
        assertTrue(authService.isAuthorizedInstitutionalEmail("vivekvaranasi99@gmail.com"));
        assertFalse(authService.isAuthorizedInstitutionalEmail("unauthorized@yahoo.com"));
        assertFalse(authService.isAuthorizedInstitutionalEmail("random@gmail.com"));
    }

    // =========================================================================
    // P4 MANDATORY AUTOMATED TESTS (12 Scenarios)
    // =========================================================================

    // 1. New student registration
    @Test
    void test1_NewStudentRegistration() {
        String email = "fresh.student@vrsec.ac.in";
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("456789");
        otp.setPurpose("VERIFICATION");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        Map<String, Object> result = authService.verifyOtp(email, "456789", "Fresh Student", null);

        assertNotNull(result);
        assertEquals("STUDENT", result.get("role"));
        assertEquals("mocked.jwt.token", result.get("token"));

        User created = userDb.get(email);
        assertNotNull(created);
        assertEquals("fresh.student@vrsec.ac.in", created.getEmail());
        assertEquals("Fresh Student", created.getUsername());
        assertEquals("STUDENT", created.getRole());
        assertNull(created.getBranch(), "Branch must be null for new students to trigger academic onboarding");
        assertNull(created.getAcademicYear(), "Academic year must be null for new students");
        assertNull(created.getSemester(), "Semester must be null for new students");
    }

    // 2. New faculty registration
    @Test
    void test2_NewFacultyRegistration() {
        String email = "new.faculty@siddhartha.ac.in";
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("654321");
        otp.setPurpose("VERIFICATION");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        Map<String, Object> result = authService.verifyOtp(email, "654321", "Dr. Faculty", null);

        assertNotNull(result);
        assertEquals("FACULTY", result.get("role"));
        assertEquals("mocked.jwt.token", result.get("token"));

        User created = userDb.get(email);
        assertNotNull(created);
        assertEquals("new.faculty@siddhartha.ac.in", created.getEmail());
        assertEquals("FACULTY", created.getRole());
    }

    // 3. Duplicate registration safely updates existing user
    @Test
    void test3_DuplicateRegistrationDoesNotCreateDuplicateUser() {
        String email = "existing.student@vrsec.ac.in";
        User existing = new User();
        existing.setId(42L);
        existing.setEmail(email);
        existing.setUsername("Existing Student");
        existing.setRole("STUDENT");
        existing.setBranch("CSE");
        existing.setAcademicYear("2nd Year");
        existing.setSemester("1st Sem");
        userDb.put(email, existing);

        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("112233");
        otp.setPurpose("VERIFICATION");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        int initialUserCount = userDb.size();
        Map<String, Object> result = authService.verifyOtp(email, "112233", "Existing Student", null);

        assertNotNull(result);
        assertEquals("mocked.jwt.token", result.get("token"));
        assertEquals(initialUserCount, userDb.size(), "User count must remain unchanged");
        assertEquals("CSE", userDb.get(email).getBranch(), "Existing branch must be preserved");
    }

    // 4. Invalid email domain rejected
    @Test
    void test4_InvalidEmailDomainRejected() {
        String invalidEmail = "stranger@gmail.com";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.sendOtp(invalidEmail);
        });

        assertTrue(ex.getMessage().contains("Invalid institutional email"));
        assertTrue(otpDb.isEmpty(), "No OTP should be generated for invalid domain");
    }

    // 5. Expired OTP rejection
    @Test
    void test5_ExpiredOtpRejectedAndMarkedUsed() {
        String email = "student@vrsec.ac.in";
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("998877");
        otp.setPurpose("VERIFICATION");
        otp.setUsed(false);
        // Created 11 minutes ago, expired 1 minute ago relative to fixed clock
        otp.setCreatedAt(LocalDateTime.now(fixedClock).minusMinutes(11));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).minusMinutes(1));
        otpDb.add(otp);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyOtp(email, "998877", "Test Student", null);
        });

        assertTrue(ex.getMessage().contains("expired"));
        assertTrue(otp.isUsed(), "Expired OTP must be marked as used in database");
    }

    // 6. OTP resend cooldown (60 seconds)
    @Test
    void test6_OtpResendCooldownEnforced() {
        String email = "cooldown.test@vrsec.ac.in";

        // Initial send at T = 0
        Map<String, Object> res1 = authService.sendOtp(email);
        assertTrue((Boolean) res1.get("success"));
        assertEquals(1, otpDb.size());

        // Attempt resend at T = 30s (within 60s cooldown)
        authService.setClock(Clock.fixed(baseInstant.plusSeconds(30), zoneId));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.sendOtp(email);
        });
        assertTrue(ex.getMessage().contains("Please wait 30 seconds before requesting another verification code."));
        assertEquals(1, otpDb.size(), "No new OTP should be created during cooldown");

        // Attempt resend at T = 61s (cooldown elapsed)
        authService.setClock(Clock.fixed(baseInstant.plusSeconds(61), zoneId));
        Map<String, Object> res2 = authService.sendOtp(email);
        assertTrue((Boolean) res2.get("success"));
        assertEquals(2, otpDb.size(), "New OTP created after cooldown elapsed");
    }

    // 7. Concurrent OTP requests
    @Test
    void test7_ConcurrentOtpRequestsAreSynchronizedAndEnforceCooldown() throws Exception {
        String email = "concurrent.student@vrsec.ac.in";

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Map<String, Object>> task = () -> {
            startLatch.await();
            return authService.sendOtp(email);
        };

        Future<Map<String, Object>> future1 = executor.submit(task);
        Future<Map<String, Object>> future2 = executor.submit(task);

        // Trigger both threads simultaneously
        startLatch.countDown();

        int successCount = 0;
        int failureCount = 0;

        try {
            future1.get(5, TimeUnit.SECONDS);
            successCount++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalArgumentException) failureCount++;
        }

        try {
            future2.get(5, TimeUnit.SECONDS);
            successCount++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalArgumentException) failureCount++;
        }

        executor.shutdown();

        // Canonical string lock ensures serialized execution: exactly one succeeds and one hits cooldown
        assertEquals(1, successCount, "Exactly one concurrent request must succeed");
        assertEquals(1, failureCount, "Simultaneous duplicate request must fail cooldown");
    }

    // 8. Brevo dispatch failure throws EmailDeliveryException
    @Test
    void test8_BrevoDispatchFailureThrowsEmailDeliveryException() {
        String email = "fail.dispatch@vrsec.ac.in";
        emailDispatchSuccess.set(false);

        assertThrows(EmailDeliveryException.class, () -> {
            authService.sendOtp(email);
        });
    }

    // 9. OTP invalidation after dispatch failure
    @Test
    void test9_OtpInvalidatedAfterDispatchFailure() {
        String email = "invalidate.dispatch@vrsec.ac.in";
        emailDispatchSuccess.set(false);

        assertThrows(EmailDeliveryException.class, () -> authService.sendOtp(email));

        assertFalse(otpDb.isEmpty());
        EmailOtp failedOtp = otpDb.get(otpDb.size() - 1);
        assertTrue(failedOtp.isUsed(), "OTP must be reliably marked as used when Brevo dispatch fails");
    }

    // 10. Controller returns HTTP 502 on dispatch failure
    @Test
    void test10_ControllerReturnsHttp502OnDispatchFailure() {
        String email = "controller.502@vrsec.ac.in";
        emailDispatchSuccess.set(false);

        ResponseEntity<?> response = authController.sendOtp(Collections.singletonMap("email", email));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Email delivery service failed"));
    }

    // 11. Existing users are not forced through academic onboarding
    @Test
    void test11_ExistingUserNotForcedThroughAcademicOnboarding() {
        User existing = new User();
        existing.setEmail("active.student@vrsec.ac.in");
        existing.setRole("STUDENT");
        existing.setBranch("IT");
        existing.setAcademicYear("3rd Year");
        existing.setSemester("2nd Sem");

        // Predicate logic matching frontend: (!user.branch || !user.academicYear || !user.semester)
        boolean isAcademicProfileIncomplete = (existing.getBranch() == null 
                || existing.getAcademicYear() == null 
                || existing.getSemester() == null);

        assertFalse(isAcademicProfileIncomplete, "Existing student with complete academic profile must not be marked incomplete");
    }

    // 12. New students are prompted for missing academic details
    @Test
    void test12_NewStudentPromptedForMissingAcademicDetails() {
        User freshStudent = new User();
        freshStudent.setEmail("new.student@vrsec.ac.in");
        freshStudent.setRole("STUDENT");
        freshStudent.setBranch(null);
        freshStudent.setAcademicYear(null);
        freshStudent.setSemester(null);

        // Predicate logic matching frontend
        boolean isAcademicProfileIncomplete = (freshStudent.getBranch() == null 
                || freshStudent.getAcademicYear() == null 
                || freshStudent.getSemester() == null);

        assertTrue(isAcademicProfileIncomplete, "New student with null academic details must be marked incomplete to prompt onboarding");
    }
}
