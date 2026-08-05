package org.forif_backend.infrastructure.persistence.user;

import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserApplyJpaRepository extends JpaRepository<UserApply, Long> {
    List<UserApply> findByApplier(User applier);

    UserApply findByid(Long id);

    List<UserApply> findByApplierId(Long applierId);

    @Query("""
            SELECT DISTINCT ua.applier FROM UserApply ua
            WHERE ua.applyYear = :year
              AND ua.applySemester = :semester
              AND (
                  :search IS NULL OR :search = ''
                  OR LOWER(ua.applier.userName) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(ua.applier.department) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY ua.applier.userName ASC, ua.applier.id ASC
            """)
    List<User> findApplicantsByYearSemester(@Param("year") int year,
                                            @Param("semester") int semester,
                                            @Param("search") String search);

    boolean existsByApplierIdAndApplyYearAndApplySemester(Long applierId, int applyYear, int applySemester);
}
