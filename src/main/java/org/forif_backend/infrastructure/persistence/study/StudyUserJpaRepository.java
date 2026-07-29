package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyUserJpaRepository extends JpaRepository<StudyUser, StudyUserId> {

    @Query("SELECT su FROM StudyUser su WHERE su.user.id = :userId AND su.study.id = :studyId")
    Optional<StudyUser> findByUserIdAndStudyId(@Param("userId") Long userId, @Param("studyId") Integer studyId);

    @Query("""
            SELECT su FROM StudyUser su
            JOIN FETCH su.user u
            WHERE su.study.id = :studyId
            ORDER BY u.userName ASC
            """)
    List<StudyUser> findAllByStudyId(@Param("studyId") Integer studyId);

    @Query("SELECT su FROM StudyUser su WHERE su.user.id = :userId")
    List<StudyUser> findAllByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(su) FROM StudyUser su
            WHERE su.study.actYear = :year AND su.study.actSemester = :semester
            """)
    long countBySemester(@Param("year") int year, @Param("semester") int semester);

    @Query("""
            SELECT COUNT(su) FROM StudyUser su
            WHERE su.study.actYear = :year AND su.study.actSemester = :semester
              AND su.certificateStatus = 1
            """)
    long countIssuedCertificatesBySemester(@Param("year") int year, @Param("semester") int semester);

    void deleteByStudyId(Integer studyId);

    void deleteByUserIdAndStudyId(Long userId, Integer studyId);

    @Query("""
            SELECT CASE WHEN COUNT(su) > 0 THEN true ELSE false END
            FROM StudyUser su
            WHERE su.user.id = :userId
              AND su.study.actYear = :year
              AND su.study.actSemester = :semester
            """)
    boolean existsByUserIdAndStudyYearSemester(@Param("userId") Long userId,
                                               @Param("year") int year,
                                               @Param("semester") int semester);
}
