package org.forif_backend.domain.study;

import java.util.List;
import java.util.Map;

public interface StudyAttendanceRepository {

    List<StudyAttendance> findAllByStudyId(Integer studyId);

    void save(StudyAttendance studyAttendance);

    void saveAll(List<StudyAttendance> studyAttendances);

    /**
     * 스터디 멘티별 출석(PRESENT) 횟수 집계
     * @return userId → 출석 횟수
     */
    Map<Long, Long> countPresentByStudyId(Integer studyId);
}
