package com.tanalytics.auth.service;

public class RefreshTokenAuthException extends RuntimeException {
    public RefreshTokenAuthException(String message) {
        super(message);
    }
}
