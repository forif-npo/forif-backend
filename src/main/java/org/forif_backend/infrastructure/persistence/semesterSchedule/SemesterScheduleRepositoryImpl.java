package org.forif_backend.infrastructure.persistence.semesterSchedule;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.common.SemesterSchedule;
import org.forif_backend.domain.common.SemesterScheduleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SemesterScheduleRepositoryImpl implements SemesterScheduleRepository {

    private final SemesterScheduleJpaRepository jpaRepository;

    @Override
    public List<SemesterSchedule> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<SemesterSchedule> findByYearAndSemester(int actYear, int actSemester) {
        return jpaRepository.findByActYearAndActSemester(actYear, actSemester);
    }

    @Override
    public Optional<SemesterSchedule> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void save(SemesterSchedule schedule) {
        jpaRepository.save(schedule);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
