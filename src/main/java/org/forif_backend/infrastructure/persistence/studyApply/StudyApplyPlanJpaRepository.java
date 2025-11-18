package org.forif_backend.infrastructure.persistence.studyApply;

import org.forif_backend.domain.studyApply.StudyApply;
import org.forif_backend.domain.studyApply.StudyApplyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyApplyPlanJpaRepository extends JpaRepository<StudyApplyPlan, Long> {
    List<StudyApplyPlan> findByStudyApply(StudyApply studyApply);
}
