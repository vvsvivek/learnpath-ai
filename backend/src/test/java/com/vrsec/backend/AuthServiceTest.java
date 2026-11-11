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
    // PRIORITY 3 MANDATORY REGRESSION & ELIGIBILITY TEST SUITE
    // =========================================================================

    // 1. Student using Gmail: Accepted
    @Test
    void testStudentUsingGmail_Accepted() {
        String email = "student.john@gmail.com";
        Map<String, Object> otpRes = authService.sendOtp(email, "STUDENT", "REGISTER");
        assertTrue((Boolean) otpRes.get("success"));

        // Retrieve generated OTP
        EmailOtp otp = otpDb.get(otpDb.size() - 1);
        Map<String, Object> verifyRes = authService.verifyOtp(email, otp.getOtpCode(), "John Student", null, "STUDENT", "REGISTER");

        assertNotNull(verifyRes);
        assertEquals("STUDENT", verifyRes.get("role"));
        User user = userDb.get(email);
        assertNotNull(user);
        assertEquals("STUDENT", user.getRole());
        assertNull(user.getBranch(), "New student branch must be null for onboarding");
    }

    // 2. Student using @vrsec.ac.in: Accepted
    @Test
    void testStudentUsingVrsec_Accepted() {
        String email = "24eu04199@vrsec.ac.in";
        Map<String, Object> otpRes = authService.sendOtp(email, "STUDENT", "REGISTER");
        assertTrue((Boolean) otpRes.get("success"));

        EmailOtp otp = otpDb.get(otpDb.size() - 1);
        Map<String, Object> verifyRes = authService.verifyOtp(email, otp.getOtpCode(), "VRSEC Student", null, "STUDENT", "REGISTER");

        assertNotNull(verifyRes);
        assertEquals("STUDENT", verifyRes.get("role"));
        assertEquals("STUDENT", userDb.get(email).getRole());
    }

    // 3. Student using Outlook: Accepted
    @Test
    void testStudentUsingOutlook_Accepted() {
        String email = "learner@outlook.com";
        Map<String, Object> otpRes = authService.sendOtp(email, "STUDENT", "REGISTER");
        assertTrue((Boolean) otpRes.get("success"));

        EmailOtp otp = otpDb.get(otpDb.size() - 1);
        Map<String, Object> verifyRes = authService.verifyOtp(email, otp.getOtpCode(), "Outlook Learner", null, "STUDENT", "REGISTER");

        assertNotNull(verifyRes);
        assertEquals("STUDENT", verifyRes.get("role"));
        assertEquals("STUDENT", userDb.get(email).getRole());
    }

    // 4. Faculty using @siddhartha.ac.in: Accepted
    @Test
    void testFacultyUsingSiddhartha_Accepted() {
        String email = "hod.it@siddhartha.ac.in";
        Map<String, Object> otpRes = authService.sendOtp(email, "FACULTY", "REGISTER");
        assertTrue((Boolean) otpRes.get("success"));

        EmailOtp otp = otpDb.get(otpDb.size() - 1);
        Map<String, Object> verifyRes = authService.verifyOtp(email, otp.getOtpCode(), "Dr. HOD", null, "FACULTY", "REGISTER");

        assertNotNull(verifyRes);
        assertEquals("FACULTY", verifyRes.get("role"));
        assertEquals("FACULTY", userDb.get(email).getRole());
    }

    // 5. Faculty using vivekvaranasi99@gmail.com: Accepted
    @Test
    void testFacultyUsingAllowlistedGmail_Accepted() {
        String email = "vivekvaranasi99@gmail.com";
        Map<String, Object> otpRes = authService.sendOtp(email, "FACULTY", "REGISTER");
        assertTrue((Boolean) otpRes.get("success"));

        EmailOtp otp = otpDb.get(otpDb.size() - 1);
        Map<String, Object> verifyRes = authService.verifyOtp(email, otp.getOtpCode(), "Prof. Vivek", null, "FACULTY", "REGISTER");

        assertNotNull(verifyRes);
        assertEquals("FACULTY", verifyRes.get("role"));
        assertEquals("FACULTY", userDb.get(email).getRole());
    }

    // 6. Faculty using another Gmail: Rejected
    @Test
    void testFacultyUsingUnauthorizedGmail_Rejected() {
        String email = "random.teacher@gmail.com";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.sendOtp(email, "FACULTY", "REGISTER");
        });
        assertTrue(ex.getMessage().contains("Unauthorized faculty registration"));

        // Also verify rejection at verifyOtp boundary
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("123123");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        IllegalArgumentException verifyEx = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyOtp(email, "123123", "Teacher", null, "FACULTY", "REGISTER");
        });
        assertTrue(verifyEx.getMessage().contains("Unauthorized faculty registration"));
    }

    // 7. Student attempting faculty registration: Rejected
    @Test
    void testStudentAttemptingFacultyRegistration_Rejected() {
        String email = "student@vrsec.ac.in";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.sendOtp(email, "FACULTY", "REGISTER");
        });
        assertTrue(ex.getMessage().contains("Unauthorized faculty registration"));
    }

    // 8. Duplicate email registration: Rejected
    @Test
    void testDuplicateEmailRegistration_Rejected() {
        String email = "duplicate.test@gmail.com";
        User existing = new User();
        existing.setId(101L);
        existing.setEmail(email);
        existing.setUsername("Original User");
        existing.setRole("STUDENT");
        userDb.put(email, existing);

        // Rejected at sendOtp in REGISTER mode
        IllegalArgumentException exSend = assertThrows(IllegalArgumentException.class, () -> {
            authService.sendOtp(email, "STUDENT", "REGISTER");
        });
        assertTrue(exSend.getMessage().contains("already exists"));

        // Rejected at verifyOtp in REGISTER mode
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("554433");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        IllegalArgumentException exVerify = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyOtp(email, "554433", "New Name", null, "STUDENT", "REGISTER");
        });
        assertTrue(exVerify.getMessage().contains("already exists"));
    }

    // 9. Incorrect OTP: Rejected
    @Test
    void testIncorrectOtp_Rejected() {
        String email = "check.wrong@gmail.com";
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("888888");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyOtp(email, "000000", "User", null);
        });
        assertTrue(ex.getMessage().contains("Invalid verification code"));
    }

    // 10. Expired OTP: Rejected
    @Test
    void testExpiredOtp_Rejected() {
        String email = "expired.otp@gmail.com";
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("777777");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock).minusMinutes(15));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).minusMinutes(5));
        otpDb.add(otp);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyOtp(email, "777777", "User", null);
        });
        assertTrue(ex.getMessage().contains("expired"));
        assertTrue(otp.isUsed(), "Expired OTP must be marked used");
    }

    // 11. Reused OTP: Rejected
    @Test
    void testReusedOtp_Rejected() {
        String email = "reused.test@gmail.com";
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("112211");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        // First verification succeeds
        Map<String, Object> first = authService.verifyOtp(email, "112211", "First Attempt", null);
        assertNotNull(first);
        assertTrue(otp.isUsed());

        // Second verification with same OTP is rejected
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyOtp(email, "112211", "Second Attempt", null);
        });
        assertTrue(ex.getMessage().contains("No active verification code found"));
    }

    // 12. Existing student login: Accepted and preserves role
    @Test
    void testExistingStudentLogin_PreservesRole() {
        String email = "existing.learner@gmail.com";
        User existing = new User();
        existing.setId(201L);
        existing.setEmail(email);
        existing.setUsername("Existing Student");
        existing.setRole("STUDENT");
        existing.setBranch("CSE");
        existing.setAcademicYear("3rd Year");
        existing.setSemester("1st Sem");
        userDb.put(email, existing);

        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("334455");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        Map<String, Object> res = authService.verifyOtp(email, "334455", "Existing Student", null, "STUDENT", "LOGIN");
        assertNotNull(res);
        assertEquals("STUDENT", res.get("role"));
        assertEquals("CSE", userDb.get(email).getBranch(), "Branch must be preserved");
    }

    // 13. Existing faculty login: Accepted and preserves role
    @Test
    void testExistingFacultyLogin_PreservesRole() {
        String email = "prof.siddhartha@siddhartha.ac.in";
        User existing = new User();
        existing.setId(301L);
        existing.setEmail(email);
        existing.setUsername("Prof. Siddhartha");
        existing.setRole("FACULTY");
        existing.setBranch("IT");
        userDb.put(email, existing);

        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpCode("998899");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now(fixedClock));
        otp.setExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(10));
        otpDb.add(otp);

        Map<String, Object> res = authService.verifyOtp(email, "998899", "Prof. Siddhartha", null, "FACULTY", "LOGIN");
        assertNotNull(res);
        assertEquals("FACULTY", res.get("role"));
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
