package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HackathonEvaluationJpaRepository extends JpaRepository<HackathonEvaluation, Long> {

    Optional<HackathonEvaluation> findByHackathonIdAndTargetTeamIdAndEvaluatorId(Long hackathonId, Long teamId, Long evaluatorId);

    List<HackathonEvaluation> findByHackathonIdOrderByTargetTeamIdAsc(Long hackathonId);
}
