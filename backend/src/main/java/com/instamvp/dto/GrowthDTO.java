package com.instamvp.dto;

import java.time.LocalDateTime;

/** Crescimento de um perfil entre um snapshot de baseline e o snapshot mais recente. */
public class GrowthDTO {

    private Long profileId;
    private String username;
    private int periodDays;

    private LocalDateTime baselineCapturedAt;
    private LocalDateTime latestCapturedAt;

    private Long followersStart;
    private Long followersEnd;
    private Long followersDelta;
    private Double followersGrowthPercent;

    private Long postsCountStart;
    private Long postsCountEnd;
    private Long postsCountDelta;

    private int snapshotsInPeriod;
    private boolean insufficientData;

    /** Posição no leaderboard (1 = maior crescimento). Só preenchido quando vem de /api/leaderboard. */
    private Integer rank;

    /** Média de engagementRate dos posts no período. Só preenchido quando vem de /api/leaderboard. */
    private Double avgEngagementRate;

    /** Soma de curtidas dos posts publicados no período. Só preenchido quando vem de /api/leaderboard. */
    private Long totalLikesInPeriod;

    /** Média de curtidas por post no período. Só preenchido quando vem de /api/leaderboard. */
    private Double avgLikesInPeriod;

    /** Quantidade de posts publicados no período (cadência/atividade). Só preenchido quando vem de /api/leaderboard. */
    private Integer postsInPeriod;

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public Double getAvgEngagementRate() { return avgEngagementRate; }
    public void setAvgEngagementRate(Double avgEngagementRate) { this.avgEngagementRate = avgEngagementRate; }

    public Long getTotalLikesInPeriod() { return totalLikesInPeriod; }
    public void setTotalLikesInPeriod(Long totalLikesInPeriod) { this.totalLikesInPeriod = totalLikesInPeriod; }

    public Double getAvgLikesInPeriod() { return avgLikesInPeriod; }
    public void setAvgLikesInPeriod(Double avgLikesInPeriod) { this.avgLikesInPeriod = avgLikesInPeriod; }

    public Integer getPostsInPeriod() { return postsInPeriod; }
    public void setPostsInPeriod(Integer postsInPeriod) { this.postsInPeriod = postsInPeriod; }

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getPeriodDays() { return periodDays; }
    public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }

    public LocalDateTime getBaselineCapturedAt() { return baselineCapturedAt; }
    public void setBaselineCapturedAt(LocalDateTime baselineCapturedAt) { this.baselineCapturedAt = baselineCapturedAt; }

    public LocalDateTime getLatestCapturedAt() { return latestCapturedAt; }
    public void setLatestCapturedAt(LocalDateTime latestCapturedAt) { this.latestCapturedAt = latestCapturedAt; }

    public Long getFollowersStart() { return followersStart; }
    public void setFollowersStart(Long followersStart) { this.followersStart = followersStart; }

    public Long getFollowersEnd() { return followersEnd; }
    public void setFollowersEnd(Long followersEnd) { this.followersEnd = followersEnd; }

    public Long getFollowersDelta() { return followersDelta; }
    public void setFollowersDelta(Long followersDelta) { this.followersDelta = followersDelta; }

    public Double getFollowersGrowthPercent() { return followersGrowthPercent; }
    public void setFollowersGrowthPercent(Double followersGrowthPercent) { this.followersGrowthPercent = followersGrowthPercent; }

    public Long getPostsCountStart() { return postsCountStart; }
    public void setPostsCountStart(Long postsCountStart) { this.postsCountStart = postsCountStart; }

    public Long getPostsCountEnd() { return postsCountEnd; }
    public void setPostsCountEnd(Long postsCountEnd) { this.postsCountEnd = postsCountEnd; }

    public Long getPostsCountDelta() { return postsCountDelta; }
    public void setPostsCountDelta(Long postsCountDelta) { this.postsCountDelta = postsCountDelta; }

    public int getSnapshotsInPeriod() { return snapshotsInPeriod; }
    public void setSnapshotsInPeriod(int snapshotsInPeriod) { this.snapshotsInPeriod = snapshotsInPeriod; }

    public boolean isInsufficientData() { return insufficientData; }
    public void setInsufficientData(boolean insufficientData) { this.insufficientData = insufficientData; }
}
