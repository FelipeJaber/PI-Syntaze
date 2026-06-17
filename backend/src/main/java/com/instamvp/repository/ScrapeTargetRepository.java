package com.instamvp.repository;

import com.instamvp.model.ScrapeTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapeTargetRepository extends JpaRepository<ScrapeTarget, Long> {
    Optional<ScrapeTarget> findByUsername(String username);
    List<ScrapeTarget> findByActiveTrue();
}
