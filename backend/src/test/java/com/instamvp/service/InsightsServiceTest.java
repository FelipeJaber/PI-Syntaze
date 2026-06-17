package com.instamvp.service;

import com.instamvp.dto.HashtagDTO;
import com.instamvp.model.Post;
import com.instamvp.model.Profile;
import com.instamvp.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testa a extração de hashtags em alta a partir das legendas dos posts
 * (InsightsService.getTrendingHashtags), incluindo a regra de não contar a
 * mesma hashtag duas vezes no mesmo post.
 */
@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

    @Mock
    private PostRepository postRepository;

    private InsightsService insightsService;

    @BeforeEach
    void setUp() {
        insightsService = new InsightsService(postRepository);
    }

    private Post buildPost(String username, String caption) {
        Profile profile = new Profile();
        profile.setUsername(username);

        Post post = new Post();
        post.setProfile(profile);
        post.setCaption(caption);
        post.setPostDate(LocalDateTime.now());
        return post;
    }

    @Test
    void countsHashtagAcrossDifferentProfilesAndDeduplicatesWithinSamePost() {
        List<Post> posts = List.of(
                buildPost("nike", "Lançamento novo #drop #esporte #drop"), // #drop repetida no mesmo post
                buildPost("adidas", "Confira o #drop dessa semana")
        );
        when(postRepository.findByPostDateGreaterThanEqual(any())).thenReturn(posts);

        List<HashtagDTO> hashtags = insightsService.getTrendingHashtags(7, 10);

        HashtagDTO dropTag = hashtags.stream()
                .filter(h -> h.getTag().equals("#drop"))
                .findFirst()
                .orElseThrow();

        assertEquals(2, dropTag.getPostCount()); // 1 vez por post, não 3
        assertEquals(2, dropTag.getProfileCount()); // nike + adidas
    }
}
