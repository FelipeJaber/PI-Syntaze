package com.instamvp.dto;

import com.instamvp.model.Post;
import com.instamvp.model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Testa o cálculo de engagementRate em PostDTO.from(post, followers). */
class PostDTOTest {

    private Post buildPost(Long likes, Long comments) {
        Post post = new Post();
        post.setId(1L);
        post.setInstagramPostId("abc123");
        post.setProfile(new Profile());
        post.setLikes(likes);
        post.setComments(comments);
        return post;
    }

    @Test
    void computesEngagementRateWhenFollowersIsPositive() {
        Post post = buildPost(80L, 20L);

        PostDTO dto = PostDTO.from(post, 1000L);

        // (80 + 20) / 1000 = 0.10
        assertEquals(0.10, dto.getEngagementRate(), 0.0001);
    }

    @Test
    void treatsNullLikesAndCommentsAsZero() {
        Post post = buildPost(null, null);

        PostDTO dto = PostDTO.from(post, 500L);

        assertEquals(0.0, dto.getEngagementRate(), 0.0001);
    }

    @Test
    void returnsNullEngagementRateWhenFollowersIsNullOrZero() {
        Post post = buildPost(10L, 5L);

        assertNull(PostDTO.from(post, null).getEngagementRate());
        assertNull(PostDTO.from(post, 0L).getEngagementRate());
    }
}
