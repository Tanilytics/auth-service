package com.tanalytics.auth.controller;

import com.tanalytics.auth.model.dto.AddMemberRequest;
import com.tanalytics.auth.model.dto.CreateSiteRequest;
import com.tanalytics.auth.model.dto.SiteResponse;
import com.tanalytics.auth.model.dto.UpdateSiteSettingsRequest;
import com.tanalytics.auth.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sites")
@Tag(name = "Sites", description = "Site (tenant) management and team membership")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    // ---- Sites ----

    @GetMapping
    @Operation(summary = "List all sites the authenticated user belongs to")
    public ResponseEntity<List<SiteResponse>> listSites(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(siteService.listSites(userId));
    }

    @PostMapping
    @Operation(summary = "Create a new site and generate its API key")
    public ResponseEntity<SiteResponse> createSite(
            @Valid @RequestBody CreateSiteRequest request,
            Authentication auth
    ) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.createSite(request, userId));
    }

    @PostMapping("/{siteId}/api-key/rotate")
    @PreAuthorize("@siteAuth.hasAccess(#siteId, 'ADMIN', authentication)")
    @Operation(summary = "Rotate site API key (admin only)")
    public ResponseEntity<SiteResponse> rotateApiKey(@PathVariable UUID siteId) {
        return ResponseEntity.ok(siteService.rotateApiKey(siteId));
    }

    // ---- Settings ----

    @GetMapping("/{siteId}/settings")
    @PreAuthorize("@siteAuth.hasAccess(#siteId, 'VIEWER', authentication)")
    @Operation(summary = "Get site settings")
    public ResponseEntity<SiteResponse> getSettings(@PathVariable UUID siteId) {
        return ResponseEntity.ok(siteService.getSettings(siteId));
    }

    @PutMapping("/{siteId}/settings")
    @PreAuthorize("@siteAuth.hasAccess(#siteId, 'ADMIN', authentication)")
    @Operation(summary = "Update site settings (admin only)")
    public ResponseEntity<SiteResponse> updateSettings(
            @PathVariable UUID siteId,
            @RequestBody UpdateSiteSettingsRequest request
    ) {
        return ResponseEntity.ok(siteService.updateSettings(siteId, request));
    }

    // ---- Members ----

    @GetMapping("/{siteId}/members")
    @PreAuthorize("@siteAuth.hasAccess(#siteId, 'VIEWER', authentication)")
    @Operation(summary = "List team members for a site")
    public ResponseEntity<List<Map<String, Object>>> listMembers(@PathVariable UUID siteId) {
        return ResponseEntity.ok(siteService.listMembers(siteId));
    }

    @PostMapping("/{siteId}/members")
    @PreAuthorize("@siteAuth.hasAccess(#siteId, 'ADMIN', authentication)")
    @Operation(summary = "Add or update a team member (admin only)")
    public ResponseEntity<Map<String, Object>> addMember(
            @PathVariable UUID siteId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.addMember(siteId, request));
    }
}

