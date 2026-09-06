package org.forif_backend.infrastructure.persistence.semester;

import jakarta.persistence.LockModeType;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SemesterScheduleJpaRepository extends JpaRepository<SemesterSchedule, Long> {

    List<SemesterSchedule> findByActYearAndActSemester(int actYear, int actSemester);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM SemesterSchedule s
            WHERE s.actYear = :year AND s.actSemester = :semester
            """)
    List<SemesterSchedule> findByActYearAndActSemesterForUpdate(
            @Param("year") int year,
            @Param("semester") int semester);

    Optional<SemesterSchedule> findByActYearAndActSemesterAndPhase(int actYear, int actSemester, SemesterPhase phase);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM SemesterSchedule s
            WHERE s.actYear = :year AND s.actSemester = :semester AND s.phase = :phase
            """)
    Optional<SemesterSchedule> findByActYearAndActSemesterAndPhaseForUpdate(
            @Param("year") int year,
            @Param("semester") int semester,
            @Param("phase") SemesterPhase phase);
}
