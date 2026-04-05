package com.tanalytics.auth.service;

import com.tanalytics.auth.model.Site;
import com.tanalytics.auth.repository.SiteRepository;
import com.tanalytics.auth.repository.UserRepository;
import com.tanalytics.auth.repository.UserSiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSiteRepository userSiteRepository;

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private SiteService siteService;

    @Test
    void rotateApiKeyEvictsOldAndCachesNew() throws Exception {
        UUID siteId = UUID.randomUUID();

        Site site = new Site();
        setSiteId(site, siteId);
        setCreatedAt(site, Instant.now());
        site.setName("Test");
        site.setDomain("example.com");
        site.setApiKeyHash("oldHash");
        site.setSettings(new HashMap<>());

        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
        when(apiKeyService.generateApiKey()).thenReturn("newPlainKey");
        when(apiKeyService.hashApiKey("newPlainKey")).thenReturn("newHash");
        when(siteRepository.save(site)).thenReturn(site);

        var response = siteService.rotateApiKey(siteId);

        verify(apiKeyService).evictApiKey("oldHash");
        verify(apiKeyService).cacheApiKey(site);
        ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepository).save(captor.capture());

        assertEquals("newHash", captor.getValue().getApiKeyHash());
        assertNotEquals("***redacted***", response.apiKey());
        assertEquals("newPlainKey", response.apiKey());
    }

    private static void setSiteId(Site site, UUID id) throws Exception {
        Field field = Site.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(site, id);
    }

    private static void setCreatedAt(Site site, Instant createdAt) throws Exception {
        Field field = Site.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(site, createdAt);
    }
}
