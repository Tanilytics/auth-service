package com.tanalytics.auth.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Join entity between User and Site, carrying a per-site role. */
@Entity
@Table(name = "user_sites")
public class UserSite {

    @EmbeddedId
    private UserSiteId id = new UserSiteId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("siteId")
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(nullable = false)
    private String role = "viewer";   // admin | editor | viewer

    // ---- Constructors ----

    public UserSite() {}

    public UserSite(User user, Site site, String role) {
        this.user = user;
        this.site = site;
        this.role = role;
        this.id = new UserSiteId(user.getId(), site.getId());
    }

    // ---- Getters / Setters ----

    public UserSiteId getId() { return id; }
    public User getUser() { return user; }
    public Site getSite() { return site; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // ---- Composite key ----

    @Embeddable
    public static class UserSiteId implements Serializable {

        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "site_id")
        private UUID siteId;

        public UserSiteId() {}

        public UserSiteId(UUID userId, UUID siteId) {
            this.userId = userId;
            this.siteId = siteId;
        }

        public UUID getUserId() { return userId; }
        public UUID getSiteId() { return siteId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserSiteId that)) return false;
            return Objects.equals(userId, that.userId) && Objects.equals(siteId, that.siteId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, siteId);
        }
    }
}

