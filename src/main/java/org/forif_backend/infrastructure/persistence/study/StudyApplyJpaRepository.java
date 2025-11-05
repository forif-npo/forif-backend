package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyApply;
import org.forif_backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyApplyJpaRepository extends JpaRepository<StudyApply, Integer> {
    Optional<StudyApply> findByPrimaryMentor(User user);
}
