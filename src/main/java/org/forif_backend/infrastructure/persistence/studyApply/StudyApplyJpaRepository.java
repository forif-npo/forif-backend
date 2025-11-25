package org.forif_backend.infrastructure.persistence.studyApply;

import org.forif_backend.domain.studyApply.StudyApply;
import org.forif_backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyApplyJpaRepository extends JpaRepository<StudyApply, Integer> {
    Optional<StudyApply> findByPrimaryMentor(User user);

    @Query("SELECT sa FROM StudyApply sa " +
           "WHERE sa.primaryMentor.id = :mentorId OR sa.secondaryMentor.id = :mentorId " +
           "ORDER BY sa.createdAt DESC")
    List<StudyApply> findByMentorId(@Param("mentorId") Long mentorId);
}
