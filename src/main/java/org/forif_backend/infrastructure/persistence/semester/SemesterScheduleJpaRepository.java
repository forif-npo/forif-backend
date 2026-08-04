package org.forif_backend.infrastructure.persistence.semester;

import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SemesterScheduleJpaRepository extends JpaRepository<SemesterSchedule, Long> {

    List<SemesterSchedule> findByActYearAndActSemester(int actYear, int actSemester);

    Optional<SemesterSchedule> findByActYearAndActSemesterAndPhase(int actYear, int actSemester, SemesterPhase phase);
}
