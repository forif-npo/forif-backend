package org.forif_backend.infrastructure.persistence.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffInterviewQuestionJpaRepository extends JpaRepository<StaffInterviewQuestion, Integer> {

    @Query("""
            SELECT q FROM StaffInterviewQuestion q
            LEFT JOIN FETCH q.department d
            WHERE q.recruitment.id = :recruitmentId
            ORDER BY CASE WHEN d IS NULL THEN 0 ELSE 1 END, d.displayOrder ASC, q.displayOrder ASC, q.id ASC
            """)
    List<StaffInterviewQuestion> findAllByRecruitmentId(@Param("recruitmentId") Integer recruitmentId);
}
