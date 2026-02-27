package com.tanalytics.auth.model.dto;

import java.util.Map;

public record UpdateSiteSettingsRequest(
        Map<String, Object> settings,
        Integer retentionDays,
        Integer rateLimitRps
) {}

