package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.Study;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyJpaRepository extends JpaRepository<Study, Integer> {
}
