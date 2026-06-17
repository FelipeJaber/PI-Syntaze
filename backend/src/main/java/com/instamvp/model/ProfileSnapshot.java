package com.instamvp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Foto do estado de um perfil em um instante específico. Diferente de
 * {@link Profile}, que guarda só o estado mais recente, esta tabela acumula
 * um registro por verificação do worker — é o que permite calcular
 * crescimento e tendências ao longo do tempo.
 */
@Entity
@Table(name = "profile_snapshots")
public class ProfileSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    private Long followers;

    private Long following;

    @Column(name = "posts_count")
    private Long postsCount;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @PrePersist
    public void prePersist() {
        if (this.capturedAt == null) {
            this.capturedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    public Long getFollowers() { return followers; }
    public void setFollowers(Long followers) { this.followers = followers; }

    public Long getFollowing() { return following; }
    public void setFollowing(Long following) { this.following = following; }

    public Long getPostsCount() { return postsCount; }
    public void setPostsCount(Long postsCount) { this.postsCount = postsCount; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
}
