package org.forif_backend.infrastructure.persistence.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffInterviewHostSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffInterviewHostSlotJpaRepository extends JpaRepository<StaffInterviewHostSlot, Integer> {

    @Query("""
            SELECT s FROM StaffInterviewHostSlot s
            WHERE s.recruitment.id = :recruitmentId
            ORDER BY s.slotDate ASC, s.startMinute ASC
            """)
    List<StaffInterviewHostSlot> findAllByRecruitmentId(@Param("recruitmentId") Integer recruitmentId);
}
