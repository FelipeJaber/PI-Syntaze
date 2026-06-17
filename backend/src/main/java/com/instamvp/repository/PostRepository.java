package com.instamvp.repository;

import com.instamvp.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByProfileIdOrderByPostDateDesc(Long profileId);
    Optional<Post> findByProfileIdAndInstagramPostId(Long profileId, String instagramPostId);
    List<Post> findByPostDateGreaterThanEqual(LocalDateTime since);
    List<Post> findByProfileIdAndPostDateGreaterThanEqual(Long profileId, LocalDateTime since);
}
