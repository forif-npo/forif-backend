package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonEvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HackathonEvaluationCriterionJpaRepository extends JpaRepository<HackathonEvaluationCriterion, Long> {

    Optional<HackathonEvaluationCriterion> findByHackathonIdAndId(Long hackathonId, Long id);

    List<HackathonEvaluationCriterion> findByHackathonIdOrderByDisplayOrderAsc(Long hackathonId);
}
