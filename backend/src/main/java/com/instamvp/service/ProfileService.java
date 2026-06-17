package com.instamvp.service;

import com.instamvp.dto.GrowthDTO;
import com.instamvp.dto.PostDTO;
import com.instamvp.dto.ProfileDTO;
import com.instamvp.model.Post;
import com.instamvp.model.Profile;
import com.instamvp.model.ProfileSnapshot;
import com.instamvp.repository.PostRepository;
import com.instamvp.repository.ProfileRepository;
import com.instamvp.repository.ProfileSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final ProfileSnapshotRepository profileSnapshotRepository;

    public ProfileService(ProfileRepository profileRepository, PostRepository postRepository,
                           ProfileSnapshotRepository profileSnapshotRepository) {
        this.profileRepository = profileRepository;
        this.postRepository = postRepository;
        this.profileSnapshotRepository = profileSnapshotRepository;
    }

    public List<ProfileDTO> findAll() {
        return profileRepository.findAll().stream()
                .map(ProfileDTO::from)
                .collect(Collectors.toList());
    }

    public ProfileDTO findById(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Profile not found: " + id));
        return ProfileDTO.from(profile);
    }

    public List<PostDTO> findPostsByProfileId(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Profile not found: " + id));
        return postRepository.findByProfileIdOrderByPostDateDesc(id).stream()
                .map(post -> PostDTO.from(post, profile.getFollowers()))
                .collect(Collectors.toList());
    }

    /**
     * Crescimento de um perfil nos últimos `days` dias, comparando o snapshot
     * mais antigo dentro da janela com o mais recente disponível. Se não
     * houver histórico suficiente (worker ainda não rodou o bastante),
     * retorna {@code insufficientData=true} em vez de números enganosos.
     */
    public GrowthDTO computeGrowth(Long profileId, int days) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found: " + profileId));

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<ProfileSnapshot> inWindow = profileSnapshotRepository.findSinceOrderByCapturedAtAsc(profileId, since);

        ProfileSnapshot latest = profileSnapshotRepository.findFirstByProfileIdOrderByCapturedAtDesc(profileId)
                .orElse(null);

        GrowthDTO dto = new GrowthDTO();
        dto.setProfileId(profileId);
        dto.setUsername(profile.getUsername());
        dto.setPeriodDays(days);
        dto.setSnapshotsInPeriod(inWindow.size());

        if (latest == null || inWindow.isEmpty()) {
            dto.setInsufficientData(true);
            return dto;
        }

        ProfileSnapshot baseline = inWindow.get(0);

        dto.setBaselineCapturedAt(baseline.getCapturedAt());
        dto.setLatestCapturedAt(latest.getCapturedAt());

        dto.setFollowersStart(baseline.getFollowers());
        dto.setFollowersEnd(latest.getFollowers());
        dto.setPostsCountStart(baseline.getPostsCount());
        dto.setPostsCountEnd(latest.getPostsCount());

        if (baseline.getFollowers() != null && latest.getFollowers() != null) {
            long delta = latest.getFollowers() - baseline.getFollowers();
            dto.setFollowersDelta(delta);
            dto.setFollowersGrowthPercent(
                    baseline.getFollowers() > 0 ? (delta / (double) baseline.getFollowers()) * 100.0 : null);
        }

        if (baseline.getPostsCount() != null && latest.getPostsCount() != null) {
            dto.setPostsCountDelta(latest.getPostsCount() - baseline.getPostsCount());
        }

        // Mesma janela amostral, sem comparação válida ainda (só um snapshot capturado até agora).
        dto.setInsufficientData(inWindow.size() < 2 && baseline.getId().equals(latest.getId()));

        return dto;
    }

    /**
     * Ranking de todos os perfis monitorados por crescimento de seguidores no
     * período, com engajamento médio como contexto adicional. Perfis sem
     * histórico suficiente ainda aparecem no fim da lista (insufficientData=true),
     * em vez de serem escondidos — assim dá pra ver quem ainda precisa de mais
     * ciclos do worker antes de confiar no ranking.
     */
    public List<GrowthDTO> computeLeaderboard(int days) {
        List<GrowthDTO> entries = profileRepository.findAll().stream()
                .map(profile -> {
                    GrowthDTO growth = computeGrowth(profile.getId(), days);
                    growth.setAvgEngagementRate(computeAvgEngagementRate(profile));
                    return growth;
                })
                .sorted(Comparator
                        .comparing(GrowthDTO::isInsufficientData)
                        .thenComparing(Comparator.comparing(
                                (GrowthDTO g) -> g.getFollowersGrowthPercent() == null ? Double.NEGATIVE_INFINITY
                                        : g.getFollowersGrowthPercent()).reversed()))
                .collect(Collectors.toList());

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }

    private Double computeAvgEngagementRate(Profile profile) {
        if (profile.getFollowers() == null || profile.getFollowers() <= 0) {
            return null;
        }

        List<Post> posts = postRepository.findByProfileIdOrderByPostDateDesc(profile.getId());
        if (posts.isEmpty()) {
            return null;
        }

        double sum = 0;
        int counted = 0;
        for (Post post : posts) {
            long likes = post.getLikes() != null ? post.getLikes() : 0;
            long comments = post.getComments() != null ? post.getComments() : 0;
            sum += (likes + comments) / (double) profile.getFollowers();
            counted++;
        }

        return counted > 0 ? sum / counted : null;
    }
}
