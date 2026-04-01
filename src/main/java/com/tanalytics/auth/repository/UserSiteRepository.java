package com.tanalytics.auth.repository;

import com.tanalytics.auth.model.UserSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSiteRepository extends JpaRepository<UserSite, UserSite.UserSiteId> {

    @Query("""
            SELECT us FROM UserSite us
            JOIN FETCH us.user
            WHERE us.site.id = :siteId
            """)
    List<UserSite> findBySiteId(@Param("siteId") UUID siteId);

    Optional<UserSite> findByUserIdAndSiteId(UUID userId, UUID siteId);
}

