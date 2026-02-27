package com.tanalytics.auth.security;

import com.tanalytics.auth.model.UserSite;
import com.tanalytics.auth.repository.UserSiteRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Spring Security expression helper used in @PreAuthorize annotations.
 *
 * Usage:
 *   @PreAuthorize("@siteAuth.hasAccess(#siteId, 'VIEWER')")
 *   @PreAuthorize("@siteAuth.hasAccess(#siteId, 'EDITOR')")
 *   @PreAuthorize("@siteAuth.hasAccess(#siteId, 'ADMIN')")
 */
@Component("siteAuth")
public class SiteAuthorizationService {

    private static final List<String> ROLE_HIERARCHY = List.of("VIEWER", "EDITOR", "ADMIN");

    private final UserSiteRepository userSiteRepository;

    public SiteAuthorizationService(UserSiteRepository userSiteRepository) {
        this.userSiteRepository = userSiteRepository;
    }

    public boolean hasAccess(UUID siteId, String requiredRole, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        // Global admin bypasses site-level checks
        boolean isGlobalAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isGlobalAdmin) return true;

        UUID userId = UUID.fromString(authentication.getName());
        return userSiteRepository.findById(new UserSite.UserSiteId(userId, siteId))
                .map(us -> roleCovers(us.getRole().toUpperCase(), requiredRole.toUpperCase()))
                .orElse(false);
    }

    /** Returns true if the member's role is >= the required role in the hierarchy. */
    private boolean roleCovers(String memberRole, String requiredRole) {
        return ROLE_HIERARCHY.indexOf(memberRole) >= ROLE_HIERARCHY.indexOf(requiredRole);
    }
}

