package com.tanalytics.auth.model.dto;

import java.util.UUID;

public record InternalMembershipResponse(
        UUID userId,
        UUID siteId,
        boolean allowed,
        String role
) {}
