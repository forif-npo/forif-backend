package org.forif_backend.infrastructure.persistence.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffApplicationJpaRepository extends JpaRepository<StaffApplication, Integer> {

    // 지망과 답변을 한 번에 fetch 하면 MultipleBagFetchException이 나므로 지망만 조인하고
    // 답변은 @OrderBy가 걸린 지연 로딩으로 읽는다.
    @Query("""
            SELECT DISTINCT a FROM StaffApplication a
            LEFT JOIN FETCH a.choices c
            LEFT JOIN FETCH c.department
            WHERE a.id = :applicationId
            """)
    Optional<StaffApplication> findDetailById(@Param("applicationId") Integer applicationId);

    @Query("""
            SELECT DISTINCT a FROM StaffApplication a
            LEFT JOIN FETCH a.choices c
            LEFT JOIN FETCH c.department
            WHERE a.recruitment.id = :recruitmentId AND a.applicant.id = :applicantUserId
            """)
    Optional<StaffApplication> findDetailByRecruitmentIdAndApplicantId(
            @Param("recruitmentId") Integer recruitmentId,
            @Param("applicantUserId") Long applicantUserId);

    boolean existsByRecruitmentIdAndApplicantId(Integer recruitmentId, Long applicantId);

    @Query("""
            SELECT COUNT(DISTINCT a) FROM StaffApplication a
            LEFT JOIN a.choices c
            WHERE a.recruitment.id = :recruitmentId
              AND (:departmentId IS NULL OR c.department.id = :departmentId)
              AND (:search IS NULL OR a.applicantName LIKE CONCAT('%', :search, '%'))
            """)
    long countForAdmin(@Param("recruitmentId") Integer recruitmentId,
                       @Param("departmentId") Integer departmentId,
                       @Param("search") String search);

    @Query("""
            SELECT DISTINCT a FROM StaffApplication a
            LEFT JOIN a.choices c
            WHERE a.recruitment.id = :recruitmentId
              AND (:departmentId IS NULL OR c.department.id = :departmentId)
              AND (:search IS NULL OR a.applicantName LIKE CONCAT('%', :search, '%'))
              AND (:cursor IS NULL OR a.id < :cursor)
            ORDER BY a.id DESC
            """)
    List<StaffApplication> findForAdmin(@Param("recruitmentId") Integer recruitmentId,
                                        @Param("departmentId") Integer departmentId,
                                        @Param("search") String search,
                                        @Param("cursor") Integer cursor,
                                        Pageable pageable);
}
