package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyPlanJpaRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByStudy(Study study);
    List<StudyPlan> findByStudyId(Integer studyId);
    void deleteByStudyId(Integer studyId);
}
