package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudyReferenceJpaRepository extends JpaRepository<StudyReference, UUID> {
    List<StudyReference> findByStudy(Study study);
    List<StudyReference> findByStudyId(Integer studyId);
    void deleteByStudyId(Integer studyId);
}
