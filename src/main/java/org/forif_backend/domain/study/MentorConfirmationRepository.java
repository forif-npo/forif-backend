package org.forif_backend.domain.study;

import java.util.List;
import java.util.Optional;

public interface MentorConfirmationRepository {

    List<MentorConfirmation> findAllByStudyId(Integer studyId);

    Optional<MentorConfirmation> findByStudyIdAndMentorId(Integer studyId, Long mentorId);

    /** 동일 멘토의 기존 확인서가 있으면 object key를 갱신하고, 없으면 새로 발급 이력을 남긴다. */
    void upsert(Integer studyId, Long mentorId, String confirmationObjectKey);

    /** 스터디 삭제 전에 발급 이력을 함께 제거해 FK 제약 위반을 막는다. */
    void deleteByStudyId(Integer studyId);
}
