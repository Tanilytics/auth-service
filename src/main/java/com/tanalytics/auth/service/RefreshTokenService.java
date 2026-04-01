package com.tanalytics.auth.service;

import com.tanalytics.auth.model.RefreshToken;
import com.tanalytics.auth.model.User;
import com.tanalytics.auth.repository.RefreshTokenRepository;
import com.tanalytics.auth.security.JwtService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void persistRefreshToken(User user, String refreshToken) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hashToken(refreshToken));
        entity.setExpiresAt(jwtService.extractExpiration(refreshToken));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
    }

    @Transactional
    public UUID consumeRefreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new RefreshTokenAuthException("Invalid or expired refresh token");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenAuthException("Refresh token is not active"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenAuthException("Refresh token is revoked or expired");
        }

        UUID userIdFromToken = jwtService.extractUserId(refreshToken);
        UUID userIdFromStore = stored.getUser().getId();
        if (!userIdFromStore.equals(userIdFromToken)) {
            throw new RefreshTokenAuthException("Refresh token subject mismatch");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return userIdFromToken;
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.findByUser_IdAndRevokedFalse(userId)
                .forEach(token -> token.setRevoked(true));
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredAndRevokedTokens() {
        refreshTokenRepository.deleteByRevokedTrueAndExpiresAtBefore(Instant.now().minusSeconds(86400));
    }

    String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
