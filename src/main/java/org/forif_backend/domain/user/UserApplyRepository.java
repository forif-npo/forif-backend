package org.forif_backend.domain.user;

import java.util.List;
import java.util.Optional;

import org.forif_backend.domain.user.User;

public interface UserApplyRepository {
    /**
     * 사용자 ID로 스터디 신청 목록 조회
     * @param userId 사용자 ID
     * @return 스터디 신청 목록 (최신 학기순 정렬: 연도 DESC, 학기 DESC)
     */
    List<UserApply> findAllUserApplyByUserId(Long userId);

    /** 지정 학기의 모든 신청서. 어드민 신청자 관리 목록에서 1·2순위를 행으로 펼칠 때 사용한다. */
    List<UserApply> findAllByYearSemester(int year, int semester);

    List<User> findApplicantsByYearSemester(int year, int semester, String search);

    /** 현재 학기에 1·2순위 중 하나라도 합격한 사용자. 회비 관리 대상 조회에 사용한다. */
    List<User> findAcceptedApplicantsByYearSemester(int year, int semester, String search);

    boolean existsByApplierIdAndYearSemester(Long userId, int year, int semester);

    boolean existsAcceptedByApplierIdAndYearSemester(Long userId, int year, int semester);

    Optional<UserApply> findByApplierIdAndYearSemester(Long userId, int year, int semester);

    /** 해당 스터디를 1·2순위로 지원한 신청서가 하나라도 있는지 */
    boolean existsByStudyId(Integer studyId);
}
