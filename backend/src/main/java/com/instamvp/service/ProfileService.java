package com.instamvp.service;

import com.instamvp.dto.GrowthDTO;
import com.instamvp.dto.LeaderboardSort;
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
     * Ranking de todos os perfis monitorados no período, com 4 critérios de
     * ordenação possíveis (ver {@link LeaderboardSort}):
     * <ul>
     *   <li>GROWTH — % de crescimento de seguidores (padrão);</li>
     *   <li>LIKES — total de curtidas somadas nos posts do período;</li>
     *   <li>ENGAGEMENT — taxa média de engajamento no período (melhor proxy
     *       de "retenção de audiência" disponível sem dados de churn de
     *       seguidores: mostra quem prende mais atenção por seguidor, não só
     *       quem tem mais seguidores);</li>
     *   <li>ACTIVITY — quantidade de posts publicados no período (cadência).</li>
     * </ul>
     * Perfis sem histórico suficiente para GROWTH ainda aparecem no fim da
     * lista (insufficientData=true) em vez de serem escondidos.
     */
    public List<GrowthDTO> computeLeaderboard(int days, LeaderboardSort sort) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<GrowthDTO> entries = profileRepository.findAll().stream()
                .map(profile -> {
                    GrowthDTO growth = computeGrowth(profile.getId(), days);
                    applyPeriodMetrics(growth, profile, since);
                    return growth;
                })
                .sorted(comparatorFor(sort))
                .collect(Collectors.toList());

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }

    private Comparator<GrowthDTO> comparatorFor(LeaderboardSort sort) {
        return switch (sort) {
            case LIKES -> Comparator.comparing(
                    (GrowthDTO g) -> g.getTotalLikesInPeriod() == null ? -1L : g.getTotalLikesInPeriod()).reversed();
            case ENGAGEMENT -> Comparator.comparing(
                    (GrowthDTO g) -> g.getAvgEngagementRate() == null ? -1.0 : g.getAvgEngagementRate()).reversed();
            case ACTIVITY -> Comparator.comparing(
                    (GrowthDTO g) -> g.getPostsInPeriod() == null ? -1 : g.getPostsInPeriod()).reversed();
            case GROWTH -> Comparator
                    .comparing(GrowthDTO::isInsufficientData)
                    .thenComparing(Comparator.comparing(
                            (GrowthDTO g) -> g.getFollowersGrowthPercent() == null ? Double.NEGATIVE_INFINITY
                                    : g.getFollowersGrowthPercent()).reversed());
        };
    }

    /** Calcula curtidas totais/médias, engajamento médio e nº de posts — todos restritos ao período (`since`). */
    private void applyPeriodMetrics(GrowthDTO dto, Profile profile, LocalDateTime since) {
        List<Post> posts = postRepository.findByProfileIdAndPostDateGreaterThanEqual(profile.getId(), since);

        dto.setPostsInPeriod(posts.size());

        if (posts.isEmpty()) {
            return;
        }

        long totalLikes = 0;
        double engagementSum = 0;
        int engagementCounted = 0;
        Long followers = profile.getFollowers();

        for (Post post : posts) {
            long likes = post.getLikes() != null ? post.getLikes() : 0;
            long comments = post.getComments() != null ? post.getComments() : 0;
            totalLikes += likes;

            if (followers != null && followers > 0) {
                engagementSum += (likes + comments) / (double) followers;
                engagementCounted++;
            }
        }

        dto.setTotalLikesInPeriod(totalLikes);
        dto.setAvgLikesInPeriod(totalLikes / (double) posts.size());
        if (engagementCounted > 0) {
            dto.setAvgEngagementRate(engagementSum / engagementCounted);
        }
    }
}
