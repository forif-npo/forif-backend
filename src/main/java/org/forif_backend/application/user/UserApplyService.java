package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.user.dto.StudyApplyRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserApplyService {
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;

    /**
     * 스터디 지원 메서드
     * @param userId 유저ID
     * @param request 요청 dto
     */
    public void applyStudy(Long userId, StudyApplyRequest request) {
        // 유저 조회
        User user = userRepository.findUserById(userId).orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 이번 학기에 지원한 스터디 있는지 확인
        if(userRepository.existUserApply(DateUtils.getCurrentYear(), DateUtils.getCurrentSemester(), user)) {
            throw new ForifException(ErrorCode.USER_APPLY_ALREADY_EXISTS);
        }

        // 지원 스터디 존재 확인
        studyRepository.findStudyById(request.primaryStudyId())
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));
        Optional.ofNullable(request.secondaryStudyId()).ifPresent(secondaryStudyId -> studyRepository.findStudyById(secondaryStudyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND)));

        // 지원 정보 생성
        UserApply userApply = UserApply.applyStudy(request, user);

        // 지원
        userRepository.createUserApply(userApply);
    }

    /**
     * 지원자 목록을 조회하는 메서드입니다.
     * @param page
     * @param pageSize
     * @param statusFilter 상태별 필터 (ex. 대기중, 승낙, 거절)
     * @param studyFilter 스터디 필터 (특정 스터디만 조회)
     * @param applyDateDirection 지원 날짜순 정렬 옵션 (DESC, ASC)
     * @return 지원자 정보 목록
     */
    public List<UserApplyInfo> getApplyInfo(Long page, Long pageSize, UserApplyStatus statusFilter,
                                            Long studyFilter, String applyDateDirection) {
        return null;
    }
}
