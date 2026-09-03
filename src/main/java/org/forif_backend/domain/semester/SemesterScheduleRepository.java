package org.forif_backend.domain.semester;

import java.util.List;
import java.util.Optional;

public interface SemesterScheduleRepository {

    List<SemesterSchedule> findByYearAndSemester(int actYear, int actSemester);

    /** 일정 저장과 심사 마감 처리를 직렬화하기 위해 해당 학기 일정 행을 잠근 채 조회한다. */
    List<SemesterSchedule> findByYearAndSemesterForUpdate(int actYear, int actSemester);

    Optional<SemesterSchedule> findByYearAndSemesterAndPhase(int actYear, int actSemester, SemesterPhase phase);

    /** 합불 처리 종료와 수동 심사 요청을 직렬화하기 위해 일정 행을 잠근 채 조회한다. */
    Optional<SemesterSchedule> findByYearAndSemesterAndPhaseForUpdate(
            int actYear, int actSemester, SemesterPhase phase);

    SemesterSchedule save(SemesterSchedule schedule);

    void delete(SemesterSchedule schedule);
}
