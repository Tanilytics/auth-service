package com.tanalytics.auth.service;

import com.tanalytics.auth.model.Site;
import com.tanalytics.auth.model.UserSite;
import com.tanalytics.auth.model.dto.InternalMembershipResponse;
import com.tanalytics.auth.model.dto.InternalSiteConfigResponse;
import com.tanalytics.auth.repository.SiteRepository;
import com.tanalytics.auth.repository.UserSiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InternalAuthService {

    private final SiteRepository siteRepository;
    private final UserSiteRepository userSiteRepository;

    public InternalAuthService(SiteRepository siteRepository, UserSiteRepository userSiteRepository) {
        this.siteRepository = siteRepository;
        this.userSiteRepository = userSiteRepository;
    }

    @Transactional(readOnly = true)
    public InternalSiteConfigResponse getSiteConfig(UUID siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteId));

        return new InternalSiteConfigResponse(
                site.getId(),
                site.getRetentionDays(),
                site.getRateLimitRps(),
                site.getSettings()
        );
    }

    @Transactional(readOnly = true)
    public InternalMembershipResponse getMembership(UUID siteId, UUID userId) {
        return userSiteRepository.findById(new UserSite.UserSiteId(userId, siteId))
                .map(us -> new InternalMembershipResponse(userId, siteId, true, us.getRole()))
                .orElseGet(() -> new InternalMembershipResponse(userId, siteId, false, null));
    }

    @Transactional(readOnly = true)
    public List<InternalSiteConfigResponse> getAllSiteConfigs() {
        return siteRepository.findAll().stream()
                .map(site -> new InternalSiteConfigResponse(
                        site.getId(),
                        site.getRetentionDays(),
                        site.getRateLimitRps(),
                        site.getSettings()
                ))
                .toList();
    }
}
