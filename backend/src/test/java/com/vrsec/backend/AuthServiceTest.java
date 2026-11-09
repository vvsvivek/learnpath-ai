package com.vrsec.backend;

import com.vrsec.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

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
        assertThrows(IllegalArgumentException.class, () -> {
            authService.determineRole("attacker@gmail.com");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            authService.determineRole("user@random.com");
        });
    }

    @Test
    void testInstitutionalEmailValidation() {
        assertTrue(authService.isAuthorizedInstitutionalEmail("student@vrsec.ac.in"));
        assertTrue(authService.isAuthorizedInstitutionalEmail("faculty@siddhartha.ac.in"));
        assertTrue(authService.isAuthorizedInstitutionalEmail("vivekvaranasi99@gmail.com"));
        assertFalse(authService.isAuthorizedInstitutionalEmail("unauthorized@yahoo.com"));
        assertFalse(authService.isAuthorizedInstitutionalEmail("random@gmail.com"));
    }
}
