package com.tanalytics.auth.repository;

import com.tanalytics.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser_IdAndRevokedFalse(UUID userId);

    long deleteByRevokedTrueAndExpiresAtBefore(Instant cutoff);
}
