package org.forif_backend.infrastructure.persistence.semester;

import org.forif_backend.domain.semester.ActiveSemester;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveSemesterJpaRepository extends JpaRepository<ActiveSemester, Integer> {
}
