package com.instamvp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Username que o worker de scraping deve verificar continuamente. */
@Entity
@Table(name = "scrape_targets")
public class ScrapeTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** Toggle: quando false, o worker pula este perfil sem removê-lo da lista. */
    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", nullable = false)
    private ScrapeStatus lastStatus = ScrapeStatus.PENDING;

    /** Detalhe textual da última tentativa (ex: "OK: synced profile and 100 posts"). */
    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public ScrapeStatus getLastStatus() { return lastStatus; }
    public void setLastStatus(ScrapeStatus lastStatus) { this.lastStatus = lastStatus; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
