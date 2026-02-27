package com.tanalytics.auth.model.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn   // seconds
) {}

