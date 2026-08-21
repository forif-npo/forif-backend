package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.dto.ApplyDetailInfo;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.study.StudyMentorAccess;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.application.study.dto.StudyDto;
import org.forif_backend.web.study.dto.StudyResponse;
import org.forif_backend.web.userApply.dto.ApplyStatusResponse;
import org.forif_backend.web.userApply.dto.UserApplyRequest;
import org.forif_backend.web.userApply.dto.UserApplyStatusUpdateRequest;
import org.forif_backend.web.userApply.dto.UserApplyUpdateRequest;
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
    private final SemesterService semesterService;
    private final SemesterPhaseGuard semesterPhaseGuard;
    private final StudyMentorAccess studyMentorAccess;
    private final DuesService duesService;
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
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_RECRUIT);

        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        Study study = studyRepository.findStudyById(request.studyId())
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        SemesterInfo active = semesterService.getActive();
        int year = active.actYear();
        int semester = active.actSemester();
        requireApplicableStudy(study, year, semester);

        Optional<UserApply> existingApply = userRepository
                .findUserApplyByYearAndSemesterAndUser(year, semester, user);

        if (study.isAutonomousStudy()) {
            applyAutonomousStudy(user, study, request, year, semester, existingApply);
            return;
        }

        requireRegularStudyApplicationInput(request);

        if (existingApply.filter(this::isAutonomousStudyApplication).isPresent()) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_APPLY_CONFLICT);
        }

        if (request.priority() == 1) {
            if (existingApply.isPresent()) {
                throw new ForifException(ErrorCode.ALREADY_APPLIED_PRIMARY);
            }
            UserApply userApply = UserApply.applyStudy(user, study, request.applyReason(), year, semester);
            userRepository.createUserApply(userApply);
        } else if (request.priority() == 2) {
            UserApply primaryApplication = existingApply
                    .orElseThrow(() -> new ForifException(ErrorCode.PRIMARY_NOT_APPLIED));

            if (primaryApplication.getSecondaryStudy() != null) {
                throw new ForifException(ErrorCode.ALREADY_APPLIED_SECONDARY);
            }

            primaryApplication.addSecondaryStudy(study.getId(), study.getStudyName(), request.applyReason());
        } else {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 스터디 수강 신청서 수정 메서드
     * PENDING 상태이면서 멘티 모집 종료 전인 경우에만 스터디 변경 및 지원 동기 수정이 가능합니다.
     * @param userId 유저 id
     * @param applyId 신청서 id
     * @param request 수정 요청 dto
     */
    @Transactional
    public void updateApplication(Long userId, Long applyId, UserApplyUpdateRequest request) {
        UserApply apply = getApplication(applyId);

        if (!apply.getApplier().getId().equals(userId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
        requireActiveSemesterApplication(apply);
        semesterPhaseGuard.requireNotEnded(
                SemesterPhase.MENTEE_RECRUIT, apply.getApplyYear(), apply.getApplySemester());

        Study study = studyRepository.findStudyById(request.studyId())
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        if (study.isAutonomousStudy() || isAutonomousStudyApplication(apply)) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_APPLY_CONFLICT);
        }

        if (request.priority() == 1) {
            apply.updatePrimaryApplication(study.getId(), study.getStudyName(), request.applyReason());
        } else if (request.priority() == 2) {
            if (apply.getSecondaryStudy() == null) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
            apply.updateSecondaryApplication(study.getId(), study.getStudyName(), request.applyReason());
        } else {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 본인의 활동 학기 대기 중 스터디 신청서를 멘티 모집 종료 전까지 취소합니다.
     * 신청서 행을 삭제하므로, 1·2순위가 모두 대기 상태일 때만 허용합니다.
     */
    @Transactional
    public void cancelApplication(Long userId, Long applyId) {
        UserApply apply = getApplication(applyId);

        if (!apply.getApplier().getId().equals(userId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        requireActiveSemesterApplication(apply);
        semesterPhaseGuard.requireNotEnded(
                SemesterPhase.MENTEE_RECRUIT, apply.getApplyYear(), apply.getApplySemester());

        boolean hasReviewedPrimary = apply.getPrimaryStatus() != UserApplyStatus.PENDING;
        boolean hasReviewedSecondary = apply.getSecondaryStatus() != null
                && apply.getSecondaryStatus() != UserApplyStatus.PENDING;
        if (hasReviewedPrimary || hasReviewedSecondary) {
            throw new ForifException(ErrorCode.APPLY_NOT_PENDING);
        }

        userRepository.deleteUserApply(apply);
    }

    private UserApply getApplication(Long applyId) {
        return findApplication(applyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_APPLY_NOT_FOUND));
    }

    private Optional<UserApply> findApplication(Long applyId) {
        return Optional.ofNullable(userRepository.findUserApplyById(applyId));
    }

    /**
     * 합격 처리 메서드
     * @param userId 멘토 유저 id
     * @param studyId 스터디 id
     * @param applyIds 합격 처리할 신청서 id 목록
     */
    @Transactional
    public void acceptApplications(Long userId, Integer studyId, List<Long> applyIds) {
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_REVIEW);

        Study study = getStudyIfActiveMentor(userId, studyId);

        for (Long applyId : applyIds) {
            Optional<UserApply> applyOpt = findApplication(applyId);
            if (applyOpt.isEmpty()) {
                continue;
            }
            UserApply apply = applyOpt.get();

            boolean isPrimary = apply.getPrimaryStudy() == studyId;
            boolean isSecondary = studyId.equals(apply.getSecondaryStudy());
            if (!isPrimary && !isSecondary) {
                throw new ForifException(ErrorCode.USER_NOT_APPLIED_TO_STUDY);
            }

            // 이미 해당 순위가 합격이면 스킵
            if (isPrimary && apply.getPrimaryStatus() == UserApplyStatus.ACCEPT) {
                continue;
            }
            if (isSecondary && apply.getSecondaryStatus() == UserApplyStatus.ACCEPT) {
                continue;
            }

            // 이미 1순위 합격 상태에서 2순위 합격 시도 → 스킵
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

            apply.updateStatus(studyId, UserApplyStatus.ACCEPT);
            duesService.ensureMemberCheck(study, apply.getApplier());
            duesService.registerStudyUserIfEligible(study, apply.getApplier());
        }
    }

    /**
     * 불합격 처리 메서드
     * @param userId 멘토 유저 id
     * @param studyId 스터디 id
     * @param applyIds 불합격 처리할 신청서 id 목록
     */
    @Transactional
    public void rejectApplications(Long userId, Integer studyId, List<Long> applyIds) {
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_REVIEW);

        getStudyIfActiveMentor(userId, studyId);

        for (Long applyId : applyIds) {
            Optional<UserApply> applyOpt = findApplication(applyId);
            if (applyOpt.isEmpty()) {
                continue;
            }
            UserApply apply = applyOpt.get();

            boolean isPrimary = apply.getPrimaryStudy() == studyId;
            boolean isSecondary = studyId.equals(apply.getSecondaryStudy());
            if (!isPrimary && !isSecondary) {
                throw new ForifException(ErrorCode.USER_NOT_APPLIED_TO_STUDY);
            }

            UserApplyStatus currentStatus = isPrimary ? apply.getPrimaryStatus() : apply.getSecondaryStatus();

            if (currentStatus == UserApplyStatus.REJECT) {
                continue;
            }

            if (currentStatus == UserApplyStatus.ACCEPT) {
                studyUserRepository.deleteByUserIdAndStudyId(apply.getApplier().getId(), studyId);
            }

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

        SemesterInfo active = semesterService.getActive();
        int year = active.actYear();
        int semester = active.actSemester();
        boolean menteeRecruitmentOpen = semesterPhaseGuard.isOpen(
                SemesterPhase.MENTEE_RECRUIT, year, semester);

        Optional<UserApply> applyOpt = userRepository.findUserApplyByYearAndSemesterAndUser(year, semester, user);

        if (applyOpt.isEmpty()) {
            return ApplyStatusResponse.builder()
                    .canApplyPrimary(menteeRecruitmentOpen)
                    .canApplySecondary(false)
                    .build();
        }

        UserApply apply = applyOpt.get();
        boolean hasSecondary = apply.getSecondaryStudy() != null;

        Study primaryStudy = studyRepository.findStudyByIdWithTags(apply.getPrimaryStudy())
                .orElse(null);
        StudyResponse primaryStudyResponse = primaryStudy == null
                ? null
                : StudyResponse.from(StudyDto.from(primaryStudy));
        boolean isAutonomousApplication = primaryStudy != null && primaryStudy.isAutonomousStudy();

        StudyResponse secondaryStudyResponse = hasSecondary
                ? studyRepository.findStudyByIdWithTags(apply.getSecondaryStudy())
                        .map(s -> StudyResponse.from(StudyDto.from(s)))
                        .orElse(null)
                : null;

        return ApplyStatusResponse.builder()
                .canApplyPrimary(false)
                .canApplySecondary(menteeRecruitmentOpen && !hasSecondary && !isAutonomousApplication)
                .primaryStudy(primaryStudyResponse)
                .secondaryStudy(secondaryStudyResponse)
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
        UserApply userApply = getApplication(applyId);

        // 신청서가 해당 스터디에 대한 것인지 검증
        if (userApply.getPrimaryStudy() != study.getId() && !study.getId().equals(userApply.getSecondaryStudy())) {
            throw new ForifException(ErrorCode.USER_NOT_APPLIED_TO_STUDY);
        }

        return ApplyDetailInfo.builder()
                .applyReason(getApplicationContentForStudy(userApply, study.getId()))
                .build();
    }

    /**
     * 멘토가 신청서의 상태를 변경하는 메서드입니다.
     * 이 엔드포인트에서는 ACCEPT 처리를 허용하지 않습니다. (합격은 /accept 엔드포인트 사용)
     */
    @Transactional
    public void updateApplyStatus(Long userId, Integer studyId, Long applyId, UserApplyStatusUpdateRequest request) {
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_REVIEW);

        Study study = getStudyIfActiveMentor(userId, studyId);
        UserApply userApply = getApplication(applyId);

        // 신청서가 해당 스터디에 대한 것인지 검증
        if (userApply.getPrimaryStudy() != study.getId() && !study.getId().equals(userApply.getSecondaryStudy())) {
            throw new ForifException(ErrorCode.USER_NOT_APPLIED_TO_STUDY);
        }

        // ACCEPT는 /accept 엔드포인트를 사용해야 함 (StudyUser 동기화 필요)
        if (request.status() == UserApplyStatus.ACCEPT) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        userApply.updateStatus(study.getId(), request.status());
    }

    /** 조회용. 지난 학기 스터디도 본인이 멘토였으면 볼 수 있다. */
    private Study getStudyIfMentor(Long userId, Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        studyMentorAccess.requireMentor(study, userId);
        return study;
    }

    /** 변경용. 활동 학기 스터디만 건드릴 수 있다. */
    private Study getStudyIfActiveMentor(Long userId, Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        studyMentorAccess.requireMentorOfActiveSemester(study, userId);
        return study;
    }

    private void requireActiveSemesterApplication(UserApply apply) {
        SemesterInfo active = semesterService.getActive();
        if (apply.getApplyYear() != active.actYear() || apply.getApplySemester() != active.actSemester()) {
            throw new ForifException(ErrorCode.STUDY_APPLY_NOT_IN_ACTIVE_SEMESTER);
        }
    }

    /** 모집 목록의 표시 상태와 실제 지원 가능 조건을 동일하게 유지한다. */
    private void requireApplicableStudy(Study study, int activeYear, int activeSemester) {
        boolean isActiveApprovedStudy = study.getActYear() == activeYear
                && study.getActSemester() == activeSemester
                && study.getStudyStatus() == StudyStatus.APPROVED;

        if (!isActiveApprovedStudy || study.getRecruitStatus() != RecruitStatus.APPLICABLE) {
            throw new ForifException(ErrorCode.STUDY_APPLICATION_PERIOD_ENDED);
        }
    }

    private void applyAutonomousStudy(
            User user,
            Study study,
            UserApplyRequest request,
            int year,
            int semester,
            Optional<UserApply> existingApply
    ) {
        if (existingApply.isPresent()) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_APPLY_CONFLICT);
        }

        if (request.priority() != null
                || (request.applyReason() != null && !request.applyReason().isBlank())) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        UserApply userApply = UserApply.applyStudy(user, study, null, year, semester);
        userRepository.createUserApply(userApply);
    }

    private void requireRegularStudyApplicationInput(UserApplyRequest request) {
        if (request.priority() == null
                || request.applyReason() == null
                || request.applyReason().isBlank()) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean isAutonomousStudyApplication(UserApply apply) {
        return studyRepository.findStudyById(apply.getPrimaryStudy())
                .map(Study::isAutonomousStudy)
                .orElse(false);
    }

    private String getApplicationContentForStudy(UserApply userApply, Integer studyId) {
        if (userApply.getPrimaryStudy() == studyId) {
            return userApply.getPrimaryIntro();
        } else {
            return userApply.getSecondaryIntro();
        }
    }
}
