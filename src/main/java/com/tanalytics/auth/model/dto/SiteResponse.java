package com.tanalytics.auth.model.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SiteResponse(
        UUID id,
        String name,
        String domain,
        String apiKey,          // plain-text key – returned only on creation
        Map<String, Object> settings,
        int retentionDays,
        int rateLimitRps,
        Instant createdAt
) {}

