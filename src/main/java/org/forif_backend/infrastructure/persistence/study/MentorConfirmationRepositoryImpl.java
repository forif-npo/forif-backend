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
    public void upsert(Integer studyId, Long mentorId, String confirmationObjectKey) {
        mentorConfirmationJpaRepository.upsert(studyId, mentorId, confirmationObjectKey);
    }

    @Override
    public void deleteByStudyId(Integer studyId) {
        mentorConfirmationJpaRepository.deleteByStudyId(studyId);
    }
}
