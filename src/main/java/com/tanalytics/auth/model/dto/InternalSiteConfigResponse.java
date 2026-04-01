package com.tanalytics.auth.model.dto;

import java.util.Map;
import java.util.UUID;

public record InternalSiteConfigResponse(
        UUID siteId,
        int retentionDays,
        int rateLimitRps,
        Map<String, Object> settings
) {}
