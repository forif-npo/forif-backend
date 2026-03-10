package org.forif_backend.domain.common;

import java.util.List;
import java.util.Optional;

public interface SemesterScheduleRepository {

    List<SemesterSchedule> findAll();

    List<SemesterSchedule> findByYearAndSemester(int actYear, int actSemester);

    Optional<SemesterSchedule> findById(Long id);

    void save(SemesterSchedule schedule);

    void deleteById(Long id);
}
