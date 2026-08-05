package org.forif_backend.domain.semester;

import java.util.List;
import java.util.Optional;

public interface SemesterScheduleRepository {

    List<SemesterSchedule> findByYearAndSemester(int actYear, int actSemester);

    Optional<SemesterSchedule> findByYearAndSemesterAndPhase(int actYear, int actSemester, SemesterPhase phase);

    SemesterSchedule save(SemesterSchedule schedule);

    void delete(SemesterSchedule schedule);
}
