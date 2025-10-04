package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyApplyReference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyApplyReferenceJpaRepository extends JpaRepository<StudyApplyReference, Long> {
}
