package org.forif_backend.infrastructure.persistence.semesterSchedule;

import org.forif_backend.domain.common.SemesterSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SemesterScheduleJpaRepository extends JpaRepository<SemesterSchedule, Long> {

    List<SemesterSchedule> findByActYearAndActSemester(int actYear, int actSemester);
}
