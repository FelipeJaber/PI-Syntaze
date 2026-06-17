package com.instamvp.dto;

import com.instamvp.model.Profile;

public class ProfileDTO {

    private Long id;
    private String username;
    private String fullName;
    private String bio;
    private Long followers;
    private Long following;
    private Long postsCount;

    public static ProfileDTO from(Profile profile) {
        ProfileDTO dto = new ProfileDTO();
        dto.id = profile.getId();
        dto.username = profile.getUsername();
        dto.fullName = profile.getFullName();
        dto.bio = profile.getBio();
        dto.followers = profile.getFollowers();
        dto.following = profile.getFollowing();
        dto.postsCount = profile.getPostsCount();
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getBio() { return bio; }
    public Long getFollowers() { return followers; }
    public Long getFollowing() { return following; }
    public Long getPostsCount() { return postsCount; }
}
