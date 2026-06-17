package com.instamvp.dto;

import com.instamvp.model.ScrapeStatus;
import com.instamvp.model.ScrapeTarget;

import java.time.LocalDateTime;

public class ScrapeTargetDTO {

    private Long id;
    private String username;
    private boolean active;
    private ScrapeStatus lastStatus;
    private String lastMessage;
    private LocalDateTime lastCheckedAt;

    public static ScrapeTargetDTO from(ScrapeTarget target) {
        ScrapeTargetDTO dto = new ScrapeTargetDTO();
        dto.id = target.getId();
        dto.username = target.getUsername();
        dto.active = target.isActive();
        dto.lastStatus = target.getLastStatus();
        dto.lastMessage = target.getLastMessage();
        dto.lastCheckedAt = target.getLastCheckedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public boolean isActive() { return active; }
    public ScrapeStatus getLastStatus() { return lastStatus; }
    public String getLastMessage() { return lastMessage; }
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
}
