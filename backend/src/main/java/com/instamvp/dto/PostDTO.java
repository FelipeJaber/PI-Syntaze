package com.instamvp.dto;

import com.instamvp.model.Post;

import java.time.LocalDateTime;

public class PostDTO {

    private Long id;
    private String instagramPostId;
    private String caption;
    private Long likes;
    private Long comments;
    private LocalDateTime postDate;
    private String postUrl;
    private String imageUrl;
    private Double engagementRate;

    public static PostDTO from(Post post) {
        return from(post, null);
    }

    /** @param followers seguidores atuais do perfil, usado para calcular (likes+comments)/followers. Pode ser null. */
    public static PostDTO from(Post post, Long followers) {
        PostDTO dto = new PostDTO();
        dto.id = post.getId();
        dto.instagramPostId = post.getInstagramPostId();
        dto.caption = post.getCaption();
        dto.likes = post.getLikes();
        dto.comments = post.getComments();
        dto.postDate = post.getPostDate();
        dto.postUrl = post.getPostUrl();
        dto.imageUrl = post.getImageUrl();

        if (followers != null && followers > 0) {
            long likes = post.getLikes() != null ? post.getLikes() : 0;
            long comments = post.getComments() != null ? post.getComments() : 0;
            dto.engagementRate = (likes + comments) / (double) followers;
        }

        return dto;
    }

    public Long getId() { return id; }
    public String getInstagramPostId() { return instagramPostId; }
    public String getCaption() { return caption; }
    public Long getLikes() { return likes; }
    public Long getComments() { return comments; }
    public LocalDateTime getPostDate() { return postDate; }
    public String getPostUrl() { return postUrl; }
    public String getImageUrl() { return imageUrl; }
    public Double getEngagementRate() { return engagementRate; }
}
