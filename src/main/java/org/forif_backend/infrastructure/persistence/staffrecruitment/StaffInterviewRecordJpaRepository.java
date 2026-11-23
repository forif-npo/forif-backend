package org.forif_backend.infrastructure.persistence.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffInterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffInterviewRecordJpaRepository extends JpaRepository<StaffInterviewRecord, Integer> {

    @Query("SELECT r FROM StaffInterviewRecord r WHERE r.application.recruitment.id = :recruitmentId")
    List<StaffInterviewRecord> findAllByRecruitmentId(@Param("recruitmentId") Integer recruitmentId);

    @Query("SELECT r FROM StaffInterviewRecord r WHERE r.application.id = :applicationId")
    Optional<StaffInterviewRecord> findByApplicationId(@Param("applicationId") Integer applicationId);
}
