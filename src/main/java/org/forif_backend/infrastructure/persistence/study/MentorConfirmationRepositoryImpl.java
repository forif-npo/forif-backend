package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.MentorConfirmation;
import org.forif_backend.domain.study.MentorConfirmationRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MentorConfirmationRepositoryImpl implements MentorConfirmationRepository {

    private final MentorConfirmationJpaRepository mentorConfirmationJpaRepository;

    @Override
    public List<MentorConfirmation> findAllByStudyId(Integer studyId) {
        return mentorConfirmationJpaRepository.findAllByStudyId(studyId);
    }

    @Override
    public Optional<MentorConfirmation> findByStudyIdAndMentorId(Integer studyId, Long mentorId) {
        return mentorConfirmationJpaRepository.findByStudyIdAndMentorId(studyId, mentorId);
    }

    @Override
    public void save(MentorConfirmation mentorConfirmation) {
        mentorConfirmationJpaRepository.save(mentorConfirmation);
    }
}
