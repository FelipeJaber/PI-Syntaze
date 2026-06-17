package com.instamvp.service;

import com.instamvp.dto.HashtagDTO;
import com.instamvp.dto.TopPostDTO;
import com.instamvp.model.Post;
import com.instamvp.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Inteligência de conteúdo: melhores posts e hashtags em alta entre os concorrentes monitorados. */
@Service
public class InsightsService {

    // \w não cobre acentos; UNICODE_CHARACTER_CLASS faz #promoção, #ofertão etc. funcionarem.
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)", Pattern.UNICODE_CHARACTER_CLASS);

    private final PostRepository postRepository;

    public InsightsService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /** Posts com maior engajamento ((likes+comments)/followers) publicados nos últimos `days` dias. */
    @Transactional(readOnly = true)
    public List<TopPostDTO> getTopPosts(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Post> posts = postRepository.findByPostDateGreaterThanEqual(since);

        return posts.stream()
                .map(this::toTopPostDTO)
                .sorted(Comparator.comparing(
                        (TopPostDTO d) -> d.getEngagementRate() == null ? -1.0 : d.getEngagementRate())
                        .reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private TopPostDTO toTopPostDTO(Post post) {
        TopPostDTO dto = new TopPostDTO();
        dto.setPostId(post.getId());
        dto.setProfileId(post.getProfile().getId());
        dto.setUsername(post.getProfile().getUsername());
        dto.setCaption(post.getCaption());
        dto.setLikes(post.getLikes());
        dto.setComments(post.getComments());
        dto.setPostDate(post.getPostDate());
        dto.setPostUrl(post.getPostUrl());
        dto.setImageUrl(post.getImageUrl());

        Long followers = post.getProfile().getFollowers();
        if (followers != null && followers > 0) {
            long likes = post.getLikes() != null ? post.getLikes() : 0;
            long comments = post.getComments() != null ? post.getComments() : 0;
            dto.setEngagementRate((likes + comments) / (double) followers);
        }
        return dto;
    }

    /** Hashtags mais usadas nas legendas dos posts publicados nos últimos `days` dias, entre todos os perfis monitorados. */
    @Transactional(readOnly = true)
    public List<HashtagDTO> getTrendingHashtags(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Post> posts = postRepository.findByPostDateGreaterThanEqual(since);

        Map<String, HashtagAccumulator> accumulators = new HashMap<>();
        for (Post post : posts) {
            if (post.getCaption() == null) continue;

            // Conta no máximo 1x por post (evita um post com hashtag repetida inflar o ranking).
            Set<String> tagsInThisPost = new HashSet<>();
            Matcher matcher = HASHTAG_PATTERN.matcher(post.getCaption());
            while (matcher.find()) {
                tagsInThisPost.add("#" + matcher.group(1).toLowerCase());
            }

            String username = post.getProfile().getUsername();
            for (String tag : tagsInThisPost) {
                HashtagAccumulator acc = accumulators.computeIfAbsent(tag, t -> new HashtagAccumulator());
                acc.postCount++;
                acc.usernames.add(username);
            }
        }

        return accumulators.entrySet().stream()
                .map(entry -> {
                    HashtagDTO dto = new HashtagDTO();
                    dto.setTag(entry.getKey());
                    dto.setPostCount(entry.getValue().postCount);
                    dto.setProfileCount(entry.getValue().usernames.size());
                    dto.setUsernames(new ArrayList<>(entry.getValue().usernames));
                    return dto;
                })
                .sorted(Comparator.comparingInt(HashtagDTO::getPostCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static class HashtagAccumulator {
        int postCount = 0;
        Set<String> usernames = new HashSet<>();
    }
}
