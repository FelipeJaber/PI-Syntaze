package com.instamvp.dto;

import java.time.LocalDateTime;

/** Post ranqueado por engajamento, com contexto de qual perfil é (usado em /api/insights/top-posts). */
public class TopPostDTO {

    private Long postId;
    private Long profileId;
    private String username;
    private String caption;
    private Long likes;
    private Long comments;
    private Double engagementRate;
    private LocalDateTime postDate;
    private String postUrl;
    private String imageUrl;

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Long getLikes() { return likes; }
    public void setLikes(Long likes) { this.likes = likes; }

    public Long getComments() { return comments; }
    public void setComments(Long comments) { this.comments = comments; }

    public Double getEngagementRate() { return engagementRate; }
    public void setEngagementRate(Double engagementRate) { this.engagementRate = engagementRate; }

    public LocalDateTime getPostDate() { return postDate; }
    public void setPostDate(LocalDateTime postDate) { this.postDate = postDate; }

    public String getPostUrl() { return postUrl; }
    public void setPostUrl(String postUrl) { this.postUrl = postUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
