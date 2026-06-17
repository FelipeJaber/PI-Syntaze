package com.instamvp.dto;

import java.util.List;

/** Hashtag em alta entre os concorrentes monitorados num período. */
public class HashtagDTO {

    private String tag;
    private int postCount;
    private int profileCount;
    private List<String> usernames;

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }

    public int getProfileCount() { return profileCount; }
    public void setProfileCount(int profileCount) { this.profileCount = profileCount; }

    public List<String> getUsernames() { return usernames; }
    public void setUsernames(List<String> usernames) { this.usernames = usernames; }
}
