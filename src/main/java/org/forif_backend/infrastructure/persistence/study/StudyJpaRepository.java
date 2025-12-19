package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyJpaRepository extends JpaRepository<Study, Integer> {
}
