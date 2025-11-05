package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyApply;
import org.forif_backend.domain.study.StudyApplyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyApplyPlanJpaRepository extends JpaRepository<StudyApplyPlan, Long> {
    List<StudyApplyPlan> findByStudyApply(StudyApply studyApply);
}
