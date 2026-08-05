package org.forif_backend.infrastructure.persistence.semester;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SemesterScheduleRepositoryImpl implements SemesterScheduleRepository {

    private final SemesterScheduleJpaRepository semesterScheduleJpaRepository;

    @Override
    public List<SemesterSchedule> findByYearAndSemester(int actYear, int actSemester) {
        return semesterScheduleJpaRepository.findByActYearAndActSemester(actYear, actSemester);
    }

    @Override
    public Optional<SemesterSchedule> findByYearAndSemesterAndPhase(int actYear, int actSemester, SemesterPhase phase) {
        return semesterScheduleJpaRepository.findByActYearAndActSemesterAndPhase(actYear, actSemester, phase);
    }

    @Override
    public SemesterSchedule save(SemesterSchedule schedule) {
        return semesterScheduleJpaRepository.save(schedule);
    }

    @Override
    public void delete(SemesterSchedule schedule) {
        semesterScheduleJpaRepository.delete(schedule);
    }
}
