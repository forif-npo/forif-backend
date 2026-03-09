package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.dto.ApplyDetailInfo;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.common.dto.response.PageResponse;
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
import org.forif_backend.web.userApply.dto.UserApplyStatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
    public Page<UserApplyInfo> getApplyInfo(Long userId, Integer studyId, int page, int pageSize, UserApplyStatus statusFilter,
                                                          SortDirection applyDateDirection) {
        // 멘토일 경우 스터디 조회
        Study study = getStudyIfMentor(userId, studyId);

        // 페이지 객체 생성
        Pageable pageable = PageRequest.of(page, pageSize);

        // 해당 스터디 지원 정보 조회
        return userRepository.findUserApply(study.getId(), pageable, statusFilter, applyDateDirection).map(apply -> UserApplyInfo.from(apply, study));
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
     * 멘토가 신청서의 상태를 변경하는 메서드입니다.
     * @param userId 멘토 유저 id
     * @param studyId 스터디 id
     * @param applyId 지원 id
     * @param request 상태 변경 요청 dto
     */
    @Transactional
    public void updateApplyStatus(Long userId, Integer studyId, Long applyId, UserApplyStatusUpdateRequest request) {
        // 멘토 권한 확인
        Study study = getStudyIfMentor(userId, studyId);

        // 예비 상태일 경우 순번 필수 검증
        if (request.status() == UserApplyStatus.WAITLIST && request.waitlistOrder() == null) {
            throw new ForifException(ErrorCode.WAITLIST_ORDER_REQUIRED);
        }

        // 지원서 조회
        UserApply userApply = userRepository.findUserApplyById(applyId);

        // 상태 변경
        userApply.updateStatus(study.getId(), request.status(), request.waitlistOrder());
    }

    /**
     * 합격자가 최종 신청을 포기할 경우, 예비 1번을 합격으로 승격하고 나머지 예비 순번을 당기는 메서드
     * @param userId  멘토 유저 id
     * @param studyId 스터디 id
     * @param applyId 최종 신청을 포기한 합격자의 신청서 id
     */
    @Transactional
    public void promoteWaitlist(Long userId, Integer studyId, Long applyId) {
        // 1. 멘토 권한 확인
        getStudyIfMentor(userId, studyId);

        // 2. 대상 신청서 조회 & ACCEPT 상태 검증
        UserApply cancelledApply = userRepository.findUserApplyById(applyId);
        if (!cancelledApply.isAcceptedForStudy(studyId)) {
            throw new ForifException(ErrorCode.APPLY_NOT_ACCEPTED_STATUS);
        }

        // 3. 합격 취소 → REJECT로 변경
        cancelledApply.updateStatus(studyId, UserApplyStatus.REJECT, null);

        // 4. 해당 스터디의 WAITLIST 전체 조회 → 예비 순번 오름차순 정렬
        List<UserApply> waitlist = userRepository.findWaitlistByStudyId(studyId);
        waitlist.sort(Comparator.comparing(
                apply -> apply.getWaitlistOrderForStudy(studyId),
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        if (waitlist.isEmpty()) {
            return;
        }

        // 5. 예비 1번 → ACCEPT 승격
        UserApply promoted = waitlist.get(0);
        promoted.updateStatus(studyId, UserApplyStatus.ACCEPT, null);

        // 6. 나머지 예비 순번 1씩 당김
        for (int i = 1; i < waitlist.size(); i++) {
            UserApply w = waitlist.get(i);
            int currentOrder = w.getWaitlistOrderForStudy(studyId);
            w.updateStatus(studyId, UserApplyStatus.WAITLIST, currentOrder - 1);
        }

    }

    /**
     * 합격자가 최종 등록을 완료할 경우, 다른 스터디 예비 대기열에서 자동으로 제거합니다.
     * 최종 등록이 확정된 시점에 호출해야 합니다.
     * @param userId  등록을 확정하는 멘토 유저 id
     * @param studyId 최종 등록한 스터디 id
     * @param applyId 최종 등록한 합격자의 신청서 id
     */
    @Transactional
    public void confirmEnrollment(Long userId, Integer studyId, Long applyId) {
        // 1. 멘토 권한 확인
        getStudyIfMentor(userId, studyId);

        // 2. 신청서 조회 & ACCEPT 상태 검증
        UserApply apply = userRepository.findUserApplyById(applyId);
        if (!apply.isAcceptedForStudy(studyId)) {
            throw new ForifException(ErrorCode.APPLY_NOT_ACCEPTED_STATUS);
        }

        // 3. 다른 스터디 예비 대기열에서 제거 및 순번 재정렬
        removeFromOtherWaitlistIfAccepted(apply, studyId);
    }

    /**
     * 합격 처리된 신청자가 다른 스터디의 예비 대기열에 있을 경우 제거하고 순번을 재정렬합니다.
     * @param acceptedApply 합격된 신청서
     * @param acceptedStudyId 합격된 스터디 id
     */
    private void removeFromOtherWaitlistIfAccepted(UserApply acceptedApply, Integer acceptedStudyId) {
        Integer otherStudyId = getOtherWaitlistStudyId(acceptedApply, acceptedStudyId);
        if (otherStudyId == null) {
            return;
        }

        // 순번을 먼저 저장 (status 변경 전)
        int removedOrder = acceptedApply.getWaitlistOrderForStudy(otherStudyId);

        // 다른 스터디 예비 대기열에서 제거 (REJECT)
        acceptedApply.updateStatus(otherStudyId, UserApplyStatus.REJECT, null);

        // DB에서 읽어온 목록에 acceptedApply가 아직 WAITLIST로 남아있을 수 있으므로 자신을 제외하고 순번 재정렬
        List<UserApply> waitlist = userRepository.findWaitlistByStudyId(otherStudyId);
        for (UserApply w : waitlist) {
            if (w.getId().equals(acceptedApply.getId())) continue;
            int order = w.getWaitlistOrderForStudy(otherStudyId);
            if (order > removedOrder) {
                w.updateStatus(otherStudyId, UserApplyStatus.WAITLIST, order - 1);
            }
        }
    }

    /**
     * 합격된 스터디가 아닌 다른 스터디에서 WAITLIST 상태인 경우 해당 스터디 id를 반환합니다.
     * 해당하지 않으면 null 반환.
     */
    private Integer getOtherWaitlistStudyId(UserApply apply, Integer acceptedStudyId) {
        if (apply.getPrimaryStudy() == acceptedStudyId) {
            if (apply.getSecondaryStudy() != null && apply.getSecondaryStatus() == UserApplyStatus.WAITLIST) {
                return apply.getSecondaryStudy();
            }
        } else if (apply.getSecondaryStudy() != null && apply.getSecondaryStudy().equals(acceptedStudyId)) {
            if (apply.getPrimaryStatus() == UserApplyStatus.WAITLIST) {
                return apply.getPrimaryStudy();
            }
        }
        return null;
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
