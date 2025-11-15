package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
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
     * @param userId 유저 id
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
        // 유저 조회
        User user = userRepository.findUserById(userId).orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 스터디 조회
        Study study = studyRepository.findStudyById(studyId).orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        // 유저 권한 확인(멘토인지, 운영진?)
        if(!study.getPrimaryMentor().getId().equals(userId)) {
            throw new ForifException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 해당 스터디 지원 정보 조회
        return userRepository.findUserApply(studyId, page, pageSize, statusFilter, applyDateDirection).stream()
                .map(this::toUserApplyInfo).toList();
    }

    private UserApplyInfo toUserApplyInfo(UserApply userApply) {
        return UserApplyInfo.builder()
                .primaryStudyComment(userApply.getPrimaryIntro())
                .secondaryStudyComment(userApply.getSecondaryIntro())
                .applyDate(userApply.getCreatedAt().atZone(DateUtils.ZONE_SEOUL))
                .primaryStudyStatus(userApply.getPrimaryStatus().name())
                .secondaryStudyStatus(userApply.getSecondaryStatus().name())
                .applierStudentId(userApply.getApplier().getId().toString()) //학번?
                .applierName(userApply.getApplier().getUserName())
                .primaryStudyName(userApply.getpri)
                .secondaryStudyName()
    }
}
