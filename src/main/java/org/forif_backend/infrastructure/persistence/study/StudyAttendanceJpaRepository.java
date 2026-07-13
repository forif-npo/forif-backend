package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyAttendanceJpaRepository extends JpaRepository<StudyAttendance, Long> {

    @Query("SELECT sa FROM StudyAttendance sa WHERE sa.study.id = :studyId")
    List<StudyAttendance> findAllByStudyId(@Param("studyId") Integer studyId);

    @Query("""
            SELECT sa.user.id, COUNT(sa)
            FROM StudyAttendance sa
            WHERE sa.study.id = :studyId
              AND sa.attendanceStatus = org.forif_backend.domain.study.AttendanceStatus.PRESENT
            GROUP BY sa.user.id
            """)
    List<Object[]> countPresentByStudyId(@Param("studyId") Integer studyId);
}
