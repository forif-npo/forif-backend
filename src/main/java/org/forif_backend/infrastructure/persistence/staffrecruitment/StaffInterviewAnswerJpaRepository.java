package org.forif_backend.infrastructure.persistence.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffInterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffInterviewAnswerJpaRepository extends JpaRepository<StaffInterviewAnswer, Integer> {

    @Query("SELECT a FROM StaffInterviewAnswer a WHERE a.application.id = :applicationId")
    List<StaffInterviewAnswer> findAllByApplicationId(@Param("applicationId") Integer applicationId);

    @Query("""
            SELECT a FROM StaffInterviewAnswer a
            WHERE a.application.id = :applicationId AND a.question.id = :questionId
            """)
    Optional<StaffInterviewAnswer> findOne(@Param("applicationId") Integer applicationId,
                                           @Param("questionId") Integer questionId);

    @Query("SELECT COUNT(a) > 0 FROM StaffInterviewAnswer a WHERE a.question.id = :questionId")
    boolean existsByQuestionId(@Param("questionId") Integer questionId);
}
