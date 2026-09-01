package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonEvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HackathonEvaluationScoreJpaRepository extends JpaRepository<HackathonEvaluationScore, Long> {

    void deleteByEvaluationId(Long evaluationId);

    boolean existsByCriterionId(Long criterionId);

    List<HackathonEvaluationScore> findByEvaluationIdIn(List<Long> evaluationIds);
}
