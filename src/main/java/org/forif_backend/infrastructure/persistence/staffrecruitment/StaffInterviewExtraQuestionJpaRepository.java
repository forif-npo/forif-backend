package org.forif_backend.infrastructure.persistence.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffInterviewExtraQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffInterviewExtraQuestionJpaRepository extends JpaRepository<StaffInterviewExtraQuestion, Integer> {

    @Query("""
            SELECT e FROM StaffInterviewExtraQuestion e
            WHERE e.application.id = :applicationId
            ORDER BY e.displayOrder ASC, e.id ASC
            """)
    List<StaffInterviewExtraQuestion> findAllByApplicationId(@Param("applicationId") Integer applicationId);
}
