package org.forif_backend.domain.user;

import java.util.List;

import org.forif_backend.domain.user.User;

public interface UserApplyRepository {
    /**
     * 사용자 ID로 스터디 신청 목록 조회
     * @param userId 사용자 ID
     * @return 스터디 신청 목록 (최신 학기순 정렬: 연도 DESC, 학기 DESC)
     */
    List<UserApply> findAllUserApplyByUserId(Long userId);

    List<User> findApplicantsByYearSemester(int year, int semester, String search);

    boolean existsByApplierIdAndYearSemester(Long userId, int year, int semester);

    /** 해당 스터디를 1·2순위로 지원한 신청서가 하나라도 있는지 */
    boolean existsByStudyId(Integer studyId);
}
