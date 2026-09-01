package org.forif_backend.infrastructure.persistence.semester;

import org.forif_backend.domain.semester.SemesterChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterChangeLogJpaRepository extends JpaRepository<SemesterChangeLog, Long> {
}
