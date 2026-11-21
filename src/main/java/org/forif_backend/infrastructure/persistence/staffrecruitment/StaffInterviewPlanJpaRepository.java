package org.forif_backend.infrastructure.persistence.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffInterviewPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffInterviewPlanJpaRepository extends JpaRepository<StaffInterviewPlan, Integer> {

    @Query("""
            SELECT DISTINCT plan FROM StaffInterviewPlan plan
            LEFT JOIN FETCH plan.dates
            WHERE plan.recruitment.id = :recruitmentId
            """)
    Optional<StaffInterviewPlan> findDetailByRecruitmentId(@Param("recruitmentId") Integer recruitmentId);
}
