package com.tanalytics.auth.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-that-is-at-least-32-bytes-long-for-jjwt",
            3600,
            604800,
            "auth-internal"
    );

    @Test
    void accessTokenContainsExpectedClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com", "admin", List.of(UUID.randomUUID()));

        Claims claims = jwtService.parseToken(token);
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals("admin", claims.get("role", String.class));
        assertNotNull(claims.get("siteIds"));
    }

    @Test
    void refreshTokenHasRefreshType() {
        String token = jwtService.generateRefreshToken(UUID.randomUUID());

        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isAccessToken(token));
    }

    @Test
    void serviceTokenHasInternalAudience() {
        String token = jwtService.generateServiceToken("query-service");

        assertTrue(jwtService.isServiceTokenForInternalAudience(token));
        assertEquals("query-service", jwtService.extractServiceName(token));
    }
}
