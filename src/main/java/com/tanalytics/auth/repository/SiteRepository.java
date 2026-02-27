package com.tanalytics.auth.repository;

import com.tanalytics.auth.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {

    Optional<Site> findByApiKeyHash(String apiKeyHash);

    /**
     * Returns all sites where the given user has a membership entry.
     */
    @Query("""
            SELECT s FROM Site s
            JOIN s.userSites us
            WHERE us.user.id = :userId
            """)
    List<Site> findAllByUserId(@Param("userId") UUID userId);
}

