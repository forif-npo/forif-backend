package org.forif_backend.domain.study;

import java.util.Optional;

public interface StudyUserRepository {
    /**
     * 사용자 ID와 스터디 ID로 StudyUser 조회
     * @param userId 사용자 ID
     * @param studyId 스터디 ID
     * @return StudyUser 정보
     */
    Optional<StudyUser> findByUserIdAndStudyId(Long userId, Integer studyId);
}
