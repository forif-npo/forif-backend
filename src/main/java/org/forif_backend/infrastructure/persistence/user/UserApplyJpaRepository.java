package org.forif_backend.infrastructure.persistence.user;

import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserApplyJpaRepository extends JpaRepository<UserApply, Long> {
    List<UserApply> findByApplier(User applier);

    UserApply findByid(Long id);

    List<UserApply> findByApplierId(Long applierId);

    @Query("""
            SELECT ua FROM UserApply ua
            JOIN FETCH ua.applier
            WHERE ua.applyYear = :year AND ua.applySemester = :semester
            ORDER BY ua.createdAt DESC, ua.id DESC
            """)
    List<UserApply> findAllByYearSemester(@Param("year") int year, @Param("semester") int semester);

    @Query("""
            SELECT COUNT(ua) > 0 FROM UserApply ua
            WHERE ua.primaryStudy = :studyId OR ua.secondaryStudy = :studyId
            """)
    boolean existsByStudyId(@Param("studyId") Integer studyId);

    @Query("""
            SELECT DISTINCT ua.primaryStudy FROM UserApply ua
            WHERE ua.primaryStudy IN :studyIds
            """)
    List<Integer> findPrimaryStudyIdsWithApplications(@Param("studyIds") List<Integer> studyIds);

    @Query("""
            SELECT DISTINCT ua.secondaryStudy FROM UserApply ua
            WHERE ua.secondaryStudy IN :studyIds
            """)
    List<Integer> findSecondaryStudyIdsWithApplications(@Param("studyIds") List<Integer> studyIds);

    @Query("""
            SELECT u FROM User u
            WHERE EXISTS (
                SELECT 1 FROM UserApply ua
                WHERE ua.applier = u
                  AND ua.applyYear = :year
                  AND ua.applySemester = :semester
            )
              AND (
                  :search IS NULL OR :search = ''
                  OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(u.department) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY u.userName ASC, u.id ASC
            """)
    List<User> findApplicantsByYearSemester(@Param("year") int year,
                                            @Param("semester") int semester,
                                            @Param("search") String search);

    @Query("""
            SELECT DISTINCT u FROM UserApply ua
            JOIN ua.applier u
            WHERE ua.applyYear = :year
              AND ua.applySemester = :semester
              AND (ua.primaryStatus = :acceptedStatus OR ua.secondaryStatus = :acceptedStatus)
              AND (
                  :search IS NULL OR :search = ''
                  OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(u.department) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY u.userName ASC, u.id ASC
            """)
    List<User> findAcceptedApplicantsByYearSemester(@Param("year") int year,
                                                    @Param("semester") int semester,
                                                    @Param("search") String search,
                                                    @Param("acceptedStatus") UserApplyStatus acceptedStatus);

    boolean existsByApplierIdAndApplyYearAndApplySemester(Long applierId, int applyYear, int applySemester);

    @Query("""
            SELECT COUNT(ua) > 0 FROM UserApply ua
            WHERE ua.applier.id = :userId
              AND ua.applyYear = :year
              AND ua.applySemester = :semester
              AND (ua.primaryStatus = :acceptedStatus OR ua.secondaryStatus = :acceptedStatus)
            """)
    boolean existsAcceptedByApplierIdAndYearSemester(@Param("userId") Long userId,
                                                     @Param("year") int year,
                                                     @Param("semester") int semester,
                                                     @Param("acceptedStatus") UserApplyStatus acceptedStatus);

    Optional<UserApply> findByApplier_IdAndApplyYearAndApplySemester(Long applierId, int applyYear, int applySemester);
}
