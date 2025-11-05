package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyApply;
import org.forif_backend.domain.study.StudyApplyReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyApplyReferenceJpaRepository extends JpaRepository<StudyApplyReference, Long> {
    List<StudyApplyReference> findByStudyApply(StudyApply studyApply);
}
