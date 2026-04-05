package com.tanalytics.auth.service;

import com.tanalytics.auth.model.RefreshToken;
import com.tanalytics.auth.model.User;
import com.tanalytics.auth.repository.RefreshTokenRepository;
import com.tanalytics.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void consumeRefreshTokenRevokesStoredToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String raw = "refresh-token";

        User user = new User("u@example.com", "x", "admin");
        setUserId(user, userId);

        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plusSeconds(300));
        stored.setRevoked(false);

        when(jwtService.isTokenValid(raw)).thenReturn(true);
        when(jwtService.isRefreshToken(raw)).thenReturn(true);
        when(jwtService.extractUserId(raw)).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        UUID consumedUserId = refreshTokenService.consumeRefreshToken(raw);

        assertEquals(userId, consumedUserId);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertTrue(captor.getValue().isRevoked());
    }

    @Test
    void consumeRefreshTokenRejectsRevokedToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String raw = "refresh-token";

        User user = new User("u@example.com", "x", "admin");
        setUserId(user, userId);

        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plusSeconds(300));
        stored.setRevoked(true);

        when(jwtService.isTokenValid(raw)).thenReturn(true);
        when(jwtService.isRefreshToken(raw)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThrows(RefreshTokenAuthException.class, () -> refreshTokenService.consumeRefreshToken(raw));
    }

    private static void setUserId(User user, UUID id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
