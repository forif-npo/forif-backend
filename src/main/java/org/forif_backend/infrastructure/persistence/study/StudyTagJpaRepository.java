package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyTagJpaRepository extends JpaRepository<StudyTag, Long> {
}
