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
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.userApply.dto.ApplyStatusResponse;
import org.forif_backend.web.userApply.dto.UserApplyRequest;
import org.forif_backend.web.userApply.dto.UserApplyStatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserApplyService {
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudyUserRepository studyUserRepository;

    /**
     * 스터디 지원 메서드 (건별 지원)
     * @param userId 유저 id
     * @param request 요청 dto (studyId, applyReason, priority)
     */
    @Transactional
    public void applyStudy(Long userId, UserApplyRequest request) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        Study study = studyRepository.findStudyById(request.studyId())
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        int year = DateUtils.getCurrentYear();
        int semester = DateUtils.getCurrentSemester();

        if (request.priority() == 1) {
            // 이번 학기 지원서가 이미 있으면 에러
            if (userRepository.existUserApply(year, semester, user)) {
                throw new ForifException(ErrorCode.ALREADY_APPLIED_PRIMARY);
            }
            UserApply userApply = UserApply.applyStudy(user, study, request.applyReason());
            userRepository.createUserApply(userApply);
        } else if (request.priority() == 2) {
            // 이번 학기 지원서 조회 (없으면 에러 - 1순위를 먼저 지원해야 함)
            UserApply existingApply = userRepository.findUserApplyByYearAndSemesterAndUser(year, semester, user)
                    .orElseThrow(() -> new ForifException(ErrorCode.PRIMARY_NOT_APPLIED));

            // secondaryStudy 이미 있으면 에러
            if (existingApply.getSecondaryStudy() != null) {
                throw new ForifException(ErrorCode.ALREADY_APPLIED_SECONDARY);
            }

            existingApply.addSecondaryStudy(study.getId(), study.getStudyName(), request.applyReason());
        }
    }

    /**
     * 합격 처리 메서드
     * @param userId 멘토 유저 id
     * @param studyId 스터디 id
     * @param applierIds 합격 처리할 지원자 id 목록
     */
    @Transactional
    public void acceptApplications(Long userId, Integer studyId, List<Long> applierIds) {
        Study study = getStudyIfMentor(userId, studyId);

        for (Long applyId : applierIds) {
            UserApply apply = userRepository.findUserApplyById(applyId);

            // 해당 스터디에 지원했는지 검증
            boolean isPrimary = apply.getPrimaryStudy() == studyId;
            boolean isSecondary = studyId.equals(apply.getSecondaryStudy());
            if (!isPrimary && !isSecondary) {
                throw new ForifException(ErrorCode.USER_NOT_APPLIED_TO_STUDY);
            }

            // 이미 1순위 합격이면 스킵 (2순위 무시)
            if (isPrimary && apply.getPrimaryStatus() == UserApplyStatus.ACCEPT) {
                continue;
            }
            if (isSecondary && apply.getPrimaryStatus() == UserApplyStatus.ACCEPT) {
                continue;
            }

            // 2순위 합격 상태에서 1순위 합격되면 2순위 StudyUser 삭제
            if (isPrimary && apply.getSecondaryStudy() != null
                    && apply.getSecondaryStatus() == UserApplyStatus.ACCEPT) {
                studyUserRepository.deleteByUserIdAndStudyId(
                        apply.getApplier().getId(), apply.getSecondaryStudy());
                apply.updateStatus(apply.getSecondaryStudy(), UserApplyStatus.REJECT);
            }

            // 해당 순위 status를 ACCEPT로 변경
            apply.updateStatus(studyId, UserApplyStatus.ACCEPT);

            // StudyUser 레코드 생성
            StudyUser studyUser = StudyUser.create(study, apply.getApplier());
            studyUserRepository.save(studyUser);
        }
    }

    /**
     * 불합격 처리 메서드
     * @param userId 멘토 유저 id
     * @param studyId 스터디 id
     * @param applierIds 불합격 처리할 지원서 id 목록
     */
    @Transactional
    public void rejectApplications(Long userId, Integer studyId, List<Long> applierIds) {
        getStudyIfMentor(userId, studyId);

        for (Long applyId : applierIds) {
            UserApply apply = userRepository.findUserApplyById(applyId);

            // 해당 스터디에 지원했는지 검증
            boolean isPrimary = apply.getPrimaryStudy() == studyId;
            boolean isSecondary = studyId.equals(apply.getSecondaryStudy());
            if (!isPrimary && !isSecondary) {
                throw new ForifException(ErrorCode.USER_NOT_APPLIED_TO_STUDY);
            }

            UserApplyStatus currentStatus = isPrimary ? apply.getPrimaryStatus() : apply.getSecondaryStatus();

            // 이미 불합격이면 스킵
            if (currentStatus == UserApplyStatus.REJECT) {
                continue;
            }

            // 합격 상태였다면 StudyUser 삭제
            if (currentStatus == UserApplyStatus.ACCEPT) {
                studyUserRepository.deleteByUserIdAndStudyId(apply.getApplier().getId(), studyId);
            }

            // 불합격 처리
            apply.updateStatus(studyId, UserApplyStatus.REJECT);
        }
    }

    /**
     * 현재 유저의 이번 학기 지원 상태 반환
     * @param userId 유저 id
     * @return 지원 상태 응답
     */
    public ApplyStatusResponse getApplyStatus(Long userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        int year = DateUtils.getCurrentYear();
        int semester = DateUtils.getCurrentSemester();

        Optional<UserApply> applyOpt = userRepository.findUserApplyByYearAndSemesterAndUser(year, semester, user);

        if (applyOpt.isEmpty()) {
            return ApplyStatusResponse.builder()
                    .canApplyPrimary(true)
                    .canApplySecondary(true)
                    .build();
        }

        UserApply apply = applyOpt.get();
        boolean hasSecondary = apply.getSecondaryStudy() != null;

        return ApplyStatusResponse.builder()
                .canApplyPrimary(false)
                .canApplySecondary(!hasSecondary)
                .primaryStudyName(apply.getPrimaryStudyName())
                .secondaryStudyName(hasSecondary ? apply.getSecondaryStudyName() : null)
                .build();
    }

    /**
     * 지원자 목록을 조회하는 메서드입니다.
     */
    public Page<UserApplyInfo> getApplyInfo(Long userId, Integer studyId, int page, int pageSize, UserApplyStatus statusFilter,
                                                          SortDirection applyDateDirection) {
        Study study = getStudyIfMentor(userId, studyId);
        Pageable pageable = PageRequest.of(page, pageSize);
        return userRepository.findUserApply(study.getId(), pageable, statusFilter, applyDateDirection).map(apply -> UserApplyInfo.from(apply, study));
    }

    /**
     * 지원 내역 상세 조회 메서드입니다.
     */
    public ApplyDetailInfo getApplyDetailInfo(Long userId, Integer studyId, Long applyId) {
        Study study = getStudyIfMentor(userId, studyId);
        UserApply userApply = userRepository.findUserApplyById(applyId);
        return ApplyDetailInfo.builder()
                .applyReason(getApplicationContentForStudy(userApply, study.getId()))
                .build();
    }

    /**
     * 멘토가 신청서의 상태를 변경하는 메서드입니다.
     */
    @Transactional
    public void updateApplyStatus(Long userId, Integer studyId, Long applyId, UserApplyStatusUpdateRequest request) {
        Study study = getStudyIfMentor(userId, studyId);
        UserApply userApply = userRepository.findUserApplyById(applyId);
        userApply.updateStatus(study.getId(), request.status());
    }

    private Study getStudyIfMentor(Long userId, Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        if (!study.isMentor(userId)) {
            throw new ForifException(ErrorCode.NOT_STUDY_MENTOR);
        }

        return study;
    }

    private String getApplicationContentForStudy(UserApply userApply, Integer studyId) {
        if (userApply.getPrimaryStudy() == studyId) {
            return userApply.getPrimaryIntro();
        } else {
            return userApply.getSecondaryIntro();
        }
    }
}
