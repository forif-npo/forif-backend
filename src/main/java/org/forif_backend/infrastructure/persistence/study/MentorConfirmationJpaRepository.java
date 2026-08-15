package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Optional;
import org.forif_backend.domain.study.MentorConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

interface MentorConfirmationJpaRepository extends JpaRepository<MentorConfirmation, Long> {

    List<MentorConfirmation> findAllByStudyId(Integer studyId);

    Optional<MentorConfirmation> findByStudyIdAndMentorId(Integer studyId, Long mentorId);
}
