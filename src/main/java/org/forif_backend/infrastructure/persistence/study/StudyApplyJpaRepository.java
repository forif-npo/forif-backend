package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyApply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyApplyJpaRepository extends JpaRepository<StudyApply, Integer> {
}
