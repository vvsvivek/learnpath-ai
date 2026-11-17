package com.vrsec.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String configuredSecret;

    private String activeSecret;

    private static final long DEFAULT_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (configuredSecret != null && !configuredSecret.trim().isEmpty()) {
            this.activeSecret = configuredSecret.trim();
        } else {
            // Generate cryptographic 256-bit ephemeral secret if no environment variable provided
            byte[] randomBytes = new byte[32];
            new SecureRandom().nextBytes(randomBytes);
            this.activeSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            System.out.println("[SECURITY] JWT_SECRET environment variable not set. Initialized cryptographic ephemeral key.");
        }
    }

    public String generateToken(String email, String role, Long userId) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long now = System.currentTimeMillis();
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", email);
            payload.put("role", role);
            payload.put("userId", userId);
            payload.put("iat", now / 1000);
            payload.put("exp", (now + DEFAULT_EXPIRATION_MS) / 1000);

            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(header));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(payload));
            String data = encodedHeader + "." + encodedPayload;

            String signature = hmacSha256(data, activeSecret);
            return data + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    public Map<String, Object> validateAndExtractClaims(String token) {
        try {
            if (token == null || !token.contains(".")) return null;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String data = parts[0] + "." + parts[1];
            String expectedSignature = hmacSha256(data, activeSecret);
            if (!expectedSignature.equals(parts[2])) {
                return null; // Invalid signature
            }

            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(decodedBytes, Map.class);

            if (claims.containsKey("exp")) {
                long exp = ((Number) claims.get("exp")).longValue();
                if (System.currentTimeMillis() / 1000 > exp) {
                    return null; // Expired
                }
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
    }
}
