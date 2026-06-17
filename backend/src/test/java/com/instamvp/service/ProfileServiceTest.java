package com.instamvp.service;

import com.instamvp.dto.GrowthDTO;
import com.instamvp.dto.LeaderboardSort;
import com.instamvp.model.Post;
import com.instamvp.model.Profile;
import com.instamvp.model.ProfileSnapshot;
import com.instamvp.repository.PostRepository;
import com.instamvp.repository.ProfileRepository;
import com.instamvp.repository.ProfileSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testa o cálculo de crescimento (ProfileService.computeGrowth), incluindo o
 * caso de histórico insuficiente. Repositórios mockados com Mockito, sem
 * subir contexto Spring nem banco real.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ProfileSnapshotRepository profileSnapshotRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(profileRepository, postRepository, profileSnapshotRepository);
    }

    private Profile buildProfile(Long id, String username) {
        Profile profile = new Profile();
        profile.setId(id);
        profile.setUsername(username);
        return profile;
    }

    private Profile buildProfile(Long id, String username, Long followers) {
        Profile profile = buildProfile(id, username);
        profile.setFollowers(followers);
        return profile;
    }

    private Post buildPost(Long likes) {
        Post post = new Post();
        post.setLikes(likes);
        post.setComments(0L);
        return post;
    }

    private ProfileSnapshot buildSnapshot(Long id, Long followers, LocalDateTime capturedAt) {
        ProfileSnapshot snapshot = new ProfileSnapshot();
        snapshot.setId(id);
        snapshot.setFollowers(followers);
        snapshot.setPostsCount(10L);
        snapshot.setCapturedAt(capturedAt);
        return snapshot;
    }

    @Test
    void computesFollowersDeltaAndPercentBetweenBaselineAndLatestSnapshot() {
        Profile profile = buildProfile(1L, "nike");
        ProfileSnapshot baseline = buildSnapshot(10L, 1000L, LocalDateTime.now().minusDays(6));
        ProfileSnapshot latest = buildSnapshot(11L, 1100L, LocalDateTime.now());

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(profileSnapshotRepository.findSinceOrderByCapturedAtAsc(anyLong(), any()))
                .thenReturn(List.of(baseline, latest));
        when(profileSnapshotRepository.findFirstByProfileIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(latest));

        GrowthDTO growth = profileService.computeGrowth(1L, 7);

        assertEquals(1000L, growth.getFollowersStart());
        assertEquals(1100L, growth.getFollowersEnd());
        assertEquals(100L, growth.getFollowersDelta());
        assertEquals(10.0, growth.getFollowersGrowthPercent(), 0.0001);
        assertFalse(growth.isInsufficientData());
    }

    @Test
    void flagsInsufficientDataWhenNoSnapshotsExistInWindow() {
        Profile profile = buildProfile(2L, "adidas");

        when(profileRepository.findById(2L)).thenReturn(Optional.of(profile));
        when(profileSnapshotRepository.findSinceOrderByCapturedAtAsc(anyLong(), any()))
                .thenReturn(List.of());
        when(profileSnapshotRepository.findFirstByProfileIdOrderByCapturedAtDesc(2L))
                .thenReturn(Optional.empty());

        GrowthDTO growth = profileService.computeGrowth(2L, 7);

        assertTrue(growth.isInsufficientData());
    }

    @Test
    void leaderboardSortedByLikesIgnoresFollowerCount() {
        Profile nike = buildProfile(1L, "nike", 1000L);
        Profile adidas = buildProfile(2L, "adidas", 2_000_000L); // muito mais seguidor, mas menos curtidas

        when(profileRepository.findAll()).thenReturn(List.of(nike, adidas));
        when(profileRepository.findById(1L)).thenReturn(Optional.of(nike));
        when(profileRepository.findById(2L)).thenReturn(Optional.of(adidas));
        when(profileSnapshotRepository.findSinceOrderByCapturedAtAsc(anyLong(), any())).thenReturn(List.of());
        when(profileSnapshotRepository.findFirstByProfileIdOrderByCapturedAtDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(postRepository.findByProfileIdAndPostDateGreaterThanEqual(eq(1L), any()))
                .thenReturn(List.of(buildPost(500L)));
        when(postRepository.findByProfileIdAndPostDateGreaterThanEqual(eq(2L), any()))
                .thenReturn(List.of(buildPost(100L)));

        List<GrowthDTO> leaderboard = profileService.computeLeaderboard(7, LeaderboardSort.LIKES);

        assertEquals("nike", leaderboard.get(0).getUsername());
        assertEquals(1, leaderboard.get(0).getRank());
        assertEquals(500L, leaderboard.get(0).getTotalLikesInPeriod());
        assertEquals("adidas", leaderboard.get(1).getUsername());
    }
}
