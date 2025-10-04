package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyApplyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyApplyPlanJpaRepository extends JpaRepository<StudyApplyPlan, Long> {
}
