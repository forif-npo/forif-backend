package org.forif_backend.infrastructure.persistence.studyApply;

import org.forif_backend.domain.studyApply.StudyApply;
import org.forif_backend.domain.studyApply.StudyApplyReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyApplyReferenceJpaRepository extends JpaRepository<StudyApplyReference, Long> {
    List<StudyApplyReference> findByStudyApply(StudyApply studyApply);
}
