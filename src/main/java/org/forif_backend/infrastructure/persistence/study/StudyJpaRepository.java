package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyTag;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyJpaRepository extends JpaRepository<Study, Integer> {

    @Query("SELECT DISTINCT s FROM Study s LEFT JOIN FETCH s.tags WHERE s.id = :studyId")
    Optional<Study> findByIdWithTags(@Param("studyId") Integer studyId);

    @Query("SELECT DISTINCT s FROM Study s LEFT JOIN FETCH s.tags WHERE s.id IN :studyIds")
    List<Study> findByIdsWithTags(@Param("studyIds") List<Integer> studyIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Study s
            SET s.recruitStatus = :recruitStatus,
                s.updatedAt = CURRENT_TIMESTAMP
            WHERE s.actYear = :actYear
              AND s.actSemester = :actSemester
              AND s.studyStatus = :studyStatus
              AND (s.recruitStatus IS NULL OR s.recruitStatus <> :recruitStatus)
            """)
    int updateRecruitStatusForApprovedStudies(
            @Param("actYear") int actYear,
            @Param("actSemester") int actSemester,
            @Param("recruitStatus") RecruitStatus recruitStatus,
            @Param("studyStatus") StudyStatus studyStatus);
}
