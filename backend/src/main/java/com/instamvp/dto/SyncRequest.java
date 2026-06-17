package com.instamvp.dto;

import java.util.List;

public class SyncRequest {

    private List<String> usernames;

    public List<String> getUsernames() { return usernames; }
    public void setUsernames(List<String> usernames) { this.usernames = usernames; }
}
