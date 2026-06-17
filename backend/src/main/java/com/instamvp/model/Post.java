package com.instamvp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instagram_post_id", nullable = false)
    private String instagramPostId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(length = 4000)
    private String caption;

    private Long likes;

    private Long comments;

    @Column(name = "post_date")
    private LocalDateTime postDate;

    @Column(name = "post_url")
    private String postUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstagramPostId() { return instagramPostId; }
    public void setInstagramPostId(String instagramPostId) { this.instagramPostId = instagramPostId; }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Long getLikes() { return likes; }
    public void setLikes(Long likes) { this.likes = likes; }

    public Long getComments() { return comments; }
    public void setComments(Long comments) { this.comments = comments; }

    public LocalDateTime getPostDate() { return postDate; }
    public void setPostDate(LocalDateTime postDate) { this.postDate = postDate; }

    public String getPostUrl() { return postUrl; }
    public void setPostUrl(String postUrl) { this.postUrl = postUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
