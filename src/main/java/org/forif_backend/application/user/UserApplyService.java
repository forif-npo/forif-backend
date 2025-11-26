package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.dto.ApplyDetailInfo;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.userApply.dto.UserApplyRequest;
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
     * @param userId 유저 id
     * @param request 요청 dto
     */
    public void applyStudy(Long userId, UserApplyRequest request) {
        // 유저 조회
        User user = userRepository.findUserById(userId).orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 이번 학기에 지원한 스터디 있는지 확인
        if(userRepository.existUserApply(DateUtils.getCurrentYear(), DateUtils.getCurrentSemester(), user)) {
            throw new ForifException(ErrorCode.USER_APPLY_ALREADY_EXISTS);
        }

        // 지원 스터디 존재 확인
        Study primaryStudy = studyRepository.findStudyById(request.primaryStudyId())
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));
        Study secondaryStudy = Optional.ofNullable(request.secondaryStudyId())
                .map(secondaryStudyId -> studyRepository.findStudyById(secondaryStudyId)
                        .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND))).orElse(null);

        // 지원 정보 생성
        UserApply userApply = UserApply.applyStudy(request, user, primaryStudy, secondaryStudy);

        // 지원
        userRepository.createUserApply(userApply);
    }

    /**
     * 지원자 목록을 조회하는 메서드입니다.
     * @param userId 유저 id
     * @param studyId 스터디 id
     * @param page 조회 페이지
     * @param pageSize 페이지 사이즈 (기본값 20)
     * @param statusFilter 상태별 필터 (ex. 대기중, 승낙, 거절)
     * @param applyDateDirection 지원 날짜순 정렬 옵션 (DESC, ASC)
     * @return 지원자 정보 목록
     */
    public List<UserApplyInfo> getApplyInfo(Long userId, Integer studyId, Long page, Long pageSize, UserApplyStatus statusFilter,
                                            SortDirection applyDateDirection) {
        // 멘토일 경우 스터디 조회
        Study study = getStudyIfMentor(userId, studyId);

        // 해당 스터디 지원 정보 조회
        return userRepository.findUserApply(study.getId(), page, pageSize, statusFilter, applyDateDirection).stream()
                .map(userApply -> UserApplyInfo.from(userApply, study)).toList();
    }

    /**
     * 지원 내역 상세 조회 메서드입니다.
     * @param userId 유저 id
     * @param studyId 스터디 id
     * @param applyId 지원 id
     * @return 지원 내역 상세
     */
    public ApplyDetailInfo getApplyDetailInfo(Long userId, Integer studyId, Long applyId) {
        // 멘토일 경우 스터디 조회
        Study study = getStudyIfMentor(userId, studyId);

        // 지원내용 조회
        UserApply userApply = userRepository.findUserApplyById(applyId);

        // 지원내용 상세 내용 반환
        return ApplyDetailInfo.builder()
                .applyReason(getApplicationContentForStudy(userApply, study.getId()))
                .build();
    }

    /**
     * 권한이 있을 경우 스터디를 조회하는 메서드
     * @param userId 유저 ID
     * @param studyId 스터디 ID
     * @return 스터디
     */
    private Study getStudyIfMentor(Long userId, Integer studyId) {
        // 스터디 조회
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        // 권한 확인
        // TODO: 운영진으로 권한 확대?
        if (!study.isMentor(userId)) {
            throw new ForifException(ErrorCode.NOT_STUDY_MENTOR);
        }

        return study;
    }

    /**
     * 조회 스터디에 따라 지원 내용을 조회하는 메서드입니다.
     * @param userApply 지원 내역
     * @param studyId 스터디 ID
     * @return 지원 내용
     */
    private String getApplicationContentForStudy(UserApply userApply, Integer studyId) {
        if(userApply.getPrimaryStudy() == studyId) {
            return userApply.getPrimaryIntro();
        }
        else {
            return userApply.getSecondaryIntro();
        }
    }
}
