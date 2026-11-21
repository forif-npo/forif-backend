package org.forif_backend.infrastructure.persistence.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffInterviewAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffInterviewAssignmentJpaRepository extends JpaRepository<StaffInterviewAssignment, Integer> {

    @Query("""
            SELECT a FROM StaffInterviewAssignment a
            WHERE a.recruitment.id = :recruitmentId
            ORDER BY a.slotDate ASC, a.startMinute ASC
            """)
    List<StaffInterviewAssignment> findAllByRecruitmentId(@Param("recruitmentId") Integer recruitmentId);

    @Query("SELECT a FROM StaffInterviewAssignment a WHERE a.application.id = :applicationId")
    Optional<StaffInterviewAssignment> findByApplicationId(@Param("applicationId") Integer applicationId);
}
