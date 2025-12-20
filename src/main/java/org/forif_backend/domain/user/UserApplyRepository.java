package org.forif_backend.domain.user;

import java.util.List;

public interface UserApplyRepository {
    /**
     * 사용자 ID로 스터디 신청 목록 조회
     * @param userId 사용자 ID
     * @return 스터디 신청 목록 (최신 학기순 정렬: 연도 DESC, 학기 DESC)
     */
    List<UserApply> findAllUserApplyByUserId(Long userId);
}
