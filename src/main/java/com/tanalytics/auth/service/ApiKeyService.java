package com.tanalytics.auth.service;

import com.tanalytics.auth.model.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Manages API key caching in Redis.
 *
 * Key: api_key:{keyHash}
 * Value: JSON map {siteId, rateLimitRps, settings}
 * TTL: 5 minutes (gateway-service reads this cache to validate keys without
 *      calling auth-service on every request)
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "api_key:";

    private final RedisTemplate<String, Object> redis;

    public ApiKeyService(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    /** Writes / refreshes the API key entry in Redis after a site is created or updated. */
    public void cacheApiKey(Site site) {
        String redisKey = KEY_PREFIX + site.getApiKeyHash();
        Map<String, Object> value = Map.of(
                "siteId", site.getId().toString(),
                "rateLimitRps", site.getRateLimitRps(),
                "settings", site.getSettings()
        );
        redis.opsForValue().set(redisKey, value, CACHE_TTL);
        log.debug("Cached API key for site={}", site.getId());
    }

    /** Removes the API key from the Redis cache (e.g. when a key is rotated). */
    public void evictApiKey(String apiKeyHash) {
        redis.delete(KEY_PREFIX + apiKeyHash);
        log.debug("Evicted API key from cache: hash={}", apiKeyHash);
    }

    /**
     * Generates a cryptographically random API key (URL-safe Base64, 48 bytes → 64 chars).
     */
    public String generateApiKey() {
        byte[] bytes = new byte[48];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Produces a SHA-256 hex hash of the plain-text key for safe storage.
     */
    public String hashApiKey(String plainKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

