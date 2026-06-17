package com.instamvp.repository;

import com.instamvp.model.ProfileSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProfileSnapshotRepository extends JpaRepository<ProfileSnapshot, Long> {

    List<ProfileSnapshot> findByProfileIdOrderByCapturedAtAsc(Long profileId);

    Optional<ProfileSnapshot> findFirstByProfileIdOrderByCapturedAtDesc(Long profileId);

    /** Snapshot mais antigo que ainda esteja dentro da janela pedida (>= since), usado como baseline de comparação. */
    @Query("SELECT s FROM ProfileSnapshot s WHERE s.profile.id = :profileId AND s.capturedAt >= :since "
            + "ORDER BY s.capturedAt ASC")
    List<ProfileSnapshot> findSinceOrderByCapturedAtAsc(@Param("profileId") Long profileId,
                                                         @Param("since") LocalDateTime since);
}
