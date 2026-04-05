-- V1: Initial schema for auth-service
-- Managed by Flyway. auth-service owns all tables below.

-- Enable pgcrypto for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -------------------------------------------------------------------------
-- Sites (tenants)
-- -------------------------------------------------------------------------
CREATE TABLE sites (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    domain           VARCHAR(255) NOT NULL,
    api_key_hash     VARCHAR(128) NOT NULL,
    settings         JSONB        NOT NULL DEFAULT '{}',
    retention_days   INT          NOT NULL DEFAULT 365,
    rate_limit_rps   INT          NOT NULL DEFAULT 100,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sites_domain ON sites (domain);

-- -------------------------------------------------------------------------
-- Users
-- -------------------------------------------------------------------------
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'viewer',   -- admin | editor | viewer
    verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);

-- -------------------------------------------------------------------------
-- User ↔ Site membership (many-to-many with per-site role)
-- -------------------------------------------------------------------------
CREATE TABLE user_sites (
    user_id  UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    site_id  UUID        NOT NULL REFERENCES sites(id)  ON DELETE CASCADE,
    role     VARCHAR(20) NOT NULL DEFAULT 'viewer',    -- admin | editor | viewer
    PRIMARY KEY (user_id, site_id)
);

CREATE INDEX idx_user_sites_site_id ON user_sites (site_id);

-- -------------------------------------------------------------------------
-- Refresh tokens (stored for revocation support)
-- -------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id   ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

