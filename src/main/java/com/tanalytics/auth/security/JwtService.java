package com.tanalytics.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Collection;
import java.util.UUID;

/**
 * Handles JWT access token issuance and validation.
 * Both access and refresh tokens are signed with the same HMAC-SHA256 key;
 * the "type" claim distinguishes them.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessExpirySeconds;
    private final long refreshExpirySeconds;
    private final String internalAudience;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-seconds:3600}") long accessExpirySeconds,
            @Value("${jwt.refresh-token-expiry-seconds:604800}") long refreshExpirySeconds,
            @Value("${jwt.internal-audience:auth-internal}") String internalAudience
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirySeconds = accessExpirySeconds;
        this.refreshExpirySeconds = refreshExpirySeconds;
        this.internalAudience = internalAudience;
    }

    // ---- Token issuance ----

    public String generateAccessToken(UUID userId, String email, String role, List<UUID> siteIds) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .claim("siteIds", siteIds.stream().map(UUID::toString).toList())
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(accessExpirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(refreshExpirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    public String generateServiceToken(String serviceName) {
        return Jwts.builder()
                .subject(serviceName)
                .claim("type", "service")
                .claim("service", serviceName)
                .audience().add(internalAudience).and()
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(accessExpirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    // ---- Token parsing ----

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public String extractServiceName(String token) {
        Claims claims = parseToken(token);
        String service = claims.get("service", String.class);
        return service != null ? service : claims.getSubject();
    }

    public Instant extractExpiration(String token) {
        return parseToken(token).getExpiration().toInstant();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseToken(token).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseToken(token).get("type", String.class));
    }

    public boolean isServiceTokenForInternalAudience(String token) {
        Claims claims = parseToken(token);
        if (!"service".equals(claims.get("type", String.class))) {
            return false;
        }

        Collection<String> audiences = claims.getAudience();
        return audiences != null && audiences.contains(internalAudience);
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessExpirySeconds() {
        return accessExpirySeconds;
    }
}

