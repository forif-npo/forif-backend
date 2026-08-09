package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyTagJpaRepository extends JpaRepository<StudyTag, Long> {
    List<StudyTag> findByNameIn(List<String> names);
}
