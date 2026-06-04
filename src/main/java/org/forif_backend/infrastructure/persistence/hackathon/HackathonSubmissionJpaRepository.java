package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HackathonSubmissionJpaRepository extends JpaRepository<HackathonSubmission, Long> {

    Optional<HackathonSubmission> findByHackathonIdAndTeamId(Long hackathonId, Long teamId);

    List<HackathonSubmission> findByHackathonIdOrderByIdAsc(Long hackathonId);

    boolean existsByHackathonIdAndTeamId(Long hackathonId, Long teamId);
}
