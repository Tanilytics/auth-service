package com.tanalytics.auth.service;

import com.tanalytics.auth.model.User;
import com.tanalytics.auth.model.dto.LoginRequest;
import com.tanalytics.auth.repository.RefreshTokenRepository;
import com.tanalytics.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceRefreshFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User("user@example.com", "encoded", "admin");
        userRepository.save(user);

        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("user@example.com", null));
    }

    @Test
    void refreshTokenIsOneTimeUseAfterRotation() {
        var login = authService.login(new LoginRequest("user@example.com", "password"));
        String oldRefresh = login.refreshToken();

        var rotated = authService.refresh(oldRefresh);
        assertTrue(rotated.refreshToken() != null && !rotated.refreshToken().isBlank());

        assertThrows(RefreshTokenAuthException.class, () -> authService.refresh(oldRefresh));
    }
}
