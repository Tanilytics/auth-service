package com.tanalytics.auth.service;

import com.tanalytics.auth.model.Site;
import com.tanalytics.auth.model.User;
import com.tanalytics.auth.model.UserSite;
import com.tanalytics.auth.model.dto.AddMemberRequest;
import com.tanalytics.auth.model.dto.CreateSiteRequest;
import com.tanalytics.auth.model.dto.SiteResponse;
import com.tanalytics.auth.model.dto.UpdateSiteSettingsRequest;
import com.tanalytics.auth.repository.SiteRepository;
import com.tanalytics.auth.repository.UserRepository;
import com.tanalytics.auth.repository.UserSiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SiteService {

    private static final Logger log = LoggerFactory.getLogger(SiteService.class);

    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final UserSiteRepository userSiteRepository;
    private final ApiKeyService apiKeyService;

    public SiteService(
            SiteRepository siteRepository,
            UserRepository userRepository,
            UserSiteRepository userSiteRepository,
            ApiKeyService apiKeyService
    ) {
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.userSiteRepository = userSiteRepository;
        this.apiKeyService = apiKeyService;
    }

    // ---- List sites for a user ----

    @Transactional(readOnly = true)
    public List<SiteResponse> listSites(UUID userId) {
        return siteRepository.findAllByUserId(userId)
                .stream()
                .map(s -> toResponse(s, null))   // don't expose plain-text key in list
                .toList();
    }

    // ---- Create site ----

    @Transactional
    public SiteResponse createSite(CreateSiteRequest request, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + ownerId));

        String plainKey = apiKeyService.generateApiKey();
        String keyHash  = apiKeyService.hashApiKey(plainKey);

        Site site = new Site();
        site.setName(request.name());
        site.setDomain(request.domain());
        site.setApiKey(plainKey);
        site.setApiKeyHash(keyHash);
        site = siteRepository.save(site);

        // Link owner as admin member
        UserSite membership = new UserSite(owner, site, "admin");
        userSiteRepository.save(membership);

        // Populate Redis cache so gateway can validate immediately
        apiKeyService.cacheApiKey(site);

        log.info("Site created: id={} domain={} owner={}", site.getId(), site.getDomain(), ownerId);
        return toResponse(site, plainKey);  // return plain key only on creation
    }

    // ---- Get settings ----

    @Transactional(readOnly = true)
    public SiteResponse getSettings(UUID siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteId));
        return toResponse(site, null);
    }

    // ---- Update settings ----

    @Transactional
    public SiteResponse updateSettings(UUID siteId, UpdateSiteSettingsRequest request) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteId));

        if (request.settings() != null)     site.setSettings(request.settings());
        if (request.retentionDays() != null) site.setRetentionDays(request.retentionDays());
        if (request.rateLimitRps() != null)  site.setRateLimitRps(request.rateLimitRps());

        site = siteRepository.save(site);

        // Refresh Redis cache
        apiKeyService.cacheApiKey(site);

        return toResponse(site, null);
    }

    // ---- Members ----

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMembers(UUID siteId) {
        return userSiteRepository.findBySiteId(siteId).stream()
                .map(us -> Map.<String, Object>of(
                        "userId",  us.getUser().getId(),
                        "email",   us.getUser().getEmail(),
                        "role",    us.getRole()
                ))
                .toList();
    }

    @Transactional
    public Map<String, Object> addMember(UUID siteId, AddMemberRequest request) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteId));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.email()));

        UserSite.UserSiteId id = new UserSite.UserSiteId(user.getId(), siteId);
        UserSite membership = userSiteRepository.findById(id)
                .orElseGet(() -> new UserSite(user, site, request.role()));
        membership.setRole(request.role());
        userSiteRepository.save(membership);

        return Map.of("userId", user.getId(), "email", user.getEmail(), "role", request.role());
    }

    // ---- Helpers ----

    private SiteResponse toResponse(Site site, String plainKey) {
        return new SiteResponse(
                site.getId(),
                site.getName(),
                site.getDomain(),
                plainKey != null ? plainKey : "***redacted***",
                site.getSettings(),
                site.getRetentionDays(),
                site.getRateLimitRps(),
                site.getCreatedAt()
        );
    }
}

