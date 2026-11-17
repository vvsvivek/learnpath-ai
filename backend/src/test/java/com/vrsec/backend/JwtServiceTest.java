package com.vrsec.backend;

import com.vrsec.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecret = "6yGMx0SVJV0Wyrw05HEIPUbndopn4lovQdKDrFHo9UoYsNnYeDOBLNGpHJlYnpiniyljsaS0WcDCxTH9MDeB3g";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "configuredSecret", testSecret);
        jwtService.init();
    }

    @Test
    void testTokenGenerationAndValidation() {
        String token = jwtService.generateToken("student@vrsec.ac.in", "STUDENT", 1L);
        assertNotNull(token);
        assertTrue(token.contains("."));
        assertEquals(3, token.split("\\.").length);

        Map<String, Object> claims = jwtService.validateAndExtractClaims(token);
        assertNotNull(claims);
        assertEquals("student@vrsec.ac.in", claims.get("sub"));
        assertEquals("STUDENT", claims.get("role"));
        assertEquals(1, ((Number) claims.get("userId")).intValue());
    }

    @Test
    void testTamperedTokenRejection() {
        String token = jwtService.generateToken("faculty@siddhartha.ac.in", "FACULTY", 2L);
        String tampered = token.substring(0, token.length() - 5) + "ABCDE";

        Map<String, Object> claims = jwtService.validateAndExtractClaims(tampered);
        assertNull(claims, "Tampered signature must return null");
    }

    @Test
    void testEphemeralKeyFallbackWhenSecretNotProvided() {
        JwtService ephemeralJwt = new JwtService();
        ReflectionTestUtils.setField(ephemeralJwt, "configuredSecret", "");
        ephemeralJwt.init();

        String token = ephemeralJwt.generateToken("test@vrsec.ac.in", "STUDENT", 3L);
        assertNotNull(token);
        Map<String, Object> claims = ephemeralJwt.validateAndExtractClaims(token);
        assertNotNull(claims);
        assertEquals("test@vrsec.ac.in", claims.get("sub"));
    }
}
