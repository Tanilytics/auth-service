package com.tanalytics.auth.controller;

import com.tanalytics.auth.model.dto.InternalMembershipResponse;
import com.tanalytics.auth.model.dto.InternalSiteConfigResponse;
import com.tanalytics.auth.service.InternalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/auth")
@Tag(name = "Internal Auth", description = "Trusted internal service lookup endpoints")
public class InternalAuthController {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthController.class);

    private final InternalAuthService internalAuthService;

    public InternalAuthController(InternalAuthService internalAuthService) {
        this.internalAuthService = internalAuthService;
    }

    @GetMapping("/sites/{siteId}/config")
    @Operation(summary = "Get site config for trusted service callers")
    public ResponseEntity<InternalSiteConfigResponse> getSiteConfig(
            @PathVariable UUID siteId,
            Authentication authentication
    ) {
        log.debug("Internal config lookup by caller={} siteId={}", authentication.getName(), siteId);
        return ResponseEntity.ok(internalAuthService.getSiteConfig(siteId));
    }

    @GetMapping("/sites/{siteId}/users/{userId}/membership")
    @Operation(summary = "Get site membership for a user for trusted service callers")
    public ResponseEntity<InternalMembershipResponse> getMembership(
            @PathVariable UUID siteId,
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        log.debug("Internal membership lookup by caller={} siteId={} userId={}", authentication.getName(), siteId, userId);
        return ResponseEntity.ok(internalAuthService.getMembership(siteId, userId));
    }
}
