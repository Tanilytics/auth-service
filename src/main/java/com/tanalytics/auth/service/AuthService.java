package com.tanalytics.auth.service;

import com.tanalytics.auth.model.User;
import com.tanalytics.auth.model.dto.LoginRequest;
import com.tanalytics.auth.model.dto.RegisterRequest;
import com.tanalytics.auth.model.dto.TokenResponse;
import com.tanalytics.auth.model.dto.UserResponse;
import com.tanalytics.auth.repository.SiteRepository;
import com.tanalytics.auth.repository.UserRepository;
import com.tanalytics.auth.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final JwtService jwtService;
        private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            SiteRepository siteRepository,
            JwtService jwtService,
                        RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.jwtService = jwtService;
                this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    // ---- Register ----

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                "admin"   // first registered user gets admin role
        );
        user = userRepository.save(user);
        log.info("New user registered: id={} email={}", user.getId(), user.getEmail());

        return toResponse(user);
    }

    // ---- Login ----

        @Transactional
    public TokenResponse login(LoginRequest request) {
        // Delegate credential check to Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        List<UUID> siteIds = siteRepository.findAllByUserId(user.getId())
                .stream().map(s -> s.getId()).toList();

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), siteIds);
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        refreshTokenService.persistRefreshToken(user, refreshToken);

        return new TokenResponse(accessToken, refreshToken, jwtService.getAccessExpirySeconds());
    }

    // ---- Refresh token ----

        @Transactional
    public TokenResponse refresh(String refreshToken) {
                UUID userId = refreshTokenService.consumeRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        List<UUID> siteIds = siteRepository.findAllByUserId(userId)
                .stream().map(s -> s.getId()).toList();

        String newAccess  = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), siteIds);
        String newRefresh = jwtService.generateRefreshToken(user.getId());
        refreshTokenService.persistRefreshToken(user, newRefresh);

        return new TokenResponse(newAccess, newRefresh, jwtService.getAccessExpirySeconds());
    }

    // ---- Helpers ----

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}

