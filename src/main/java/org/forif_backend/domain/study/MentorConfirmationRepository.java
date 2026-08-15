package org.forif_backend.domain.study;

import java.util.List;
import java.util.Optional;

public interface MentorConfirmationRepository {

    List<MentorConfirmation> findAllByStudyId(Integer studyId);

    Optional<MentorConfirmation> findByStudyIdAndMentorId(Integer studyId, Long mentorId);

    void save(MentorConfirmation mentorConfirmation);
}
