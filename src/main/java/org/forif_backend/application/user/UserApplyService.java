package org.forif_backend.application.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.dto.response.ApiErrorData;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.application.user.dto.ApplyDetailInfo;
import org.forif_backend.application.user.dto.AdminStudyApplicationInfo;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.study.StudyMentorAccess;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.dues.MemberSemesterCheckRepository;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.type.SortCriteria;
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
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.application.study.dto.StudyDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import org.forif_backend.application.user.dto.ApplyStatusInfo;
import org.forif_backend.application.user.dto.UserApplyCommand;
import org.forif_backend.application.user.dto.UserApplyUpdateCommand;

@Service
@RequiredArgsConstructor
public class UserApplyService {
    private final SemesterService semesterService;
    private final SemesterPhaseGuard semesterPhaseGuard;
    private final StudyMentorAccess studyMentorAccess;
    private final DuesService duesService;
    private final MemberSemesterCheckRepository memberSemesterCheckRepository;
    private final UserRepository userRepository;
    private final UserApplyRepository userApplyRepository;
    private final StudyRepository studyRepository;
    private final StudyUserRepository studyUserRepository;
    private final Validator validator;

    /**
     * 스터디 지원 메서드 (건별 지원)
     * @param userId 유저 id
     * @param request 요청 dto (studyId, applyReason, priority)
     */
    @Transactional
    public void applyStudy(Long userId, UserApplyCommand request) {
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
    public void updateApplication(Long userId, Long applyId, UserApplyUpdateCommand request) {
        UserApply apply = getApplication(applyId);

        if (!apply.getApplier().getId().equals(userId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
        if (isAutonomousStudyApplication(apply)) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_APPLICATION_UPDATE_NOT_ALLOWED);
        }

        validateRegularStudyApplicationUpdateInput(request);
        requireActiveSemesterApplication(apply);
        semesterPhaseGuard.requireNotEnded(
                SemesterPhase.MENTEE_RECRUIT, apply.getApplyYear(), apply.getApplySemester());

        Study study = studyRepository.findStudyById(request.studyId())
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        requireApplicableStudy(study, apply.getApplyYear(), apply.getApplySemester());

        if (study.isAutonomousStudy()) {
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

    @Transactional(readOnly = true)
    public CursorPageResponse<AdminStudyApplicationInfo> getAdminApplications(
            int year, int semester, int page, int size, String search, List<SortCriteria> sorting
    ) {
        List<UserApply> applications = userApplyRepository.findAllByYearSemester(year, semester);
        Integer autonomousStudyId = studyRepository.findAutonomousStudyByYearSemester(year, semester)
                .map(Study::getId)
                .orElse(null);
        List<AdminStudyApplicationInfo> rows = new ArrayList<>();
        for (UserApply apply : applications) {
            rows.add(AdminStudyApplicationInfo.primary(apply, autonomousStudyId));
            if (apply.getSecondaryStudy() != null) {
                rows.add(AdminStudyApplicationInfo.secondary(apply, autonomousStudyId));
            }
        }
        String keyword = search == null ? "" : search.trim().toLowerCase();
        if (!keyword.isEmpty()) {
            rows = rows.stream().filter(row -> contains(row.userName(), keyword)
                            || contains(row.department(), keyword) || contains(row.studyName(), keyword)
                            || String.valueOf(row.userId()).contains(keyword))
                    .toList();
        }
        rows = rows.stream().sorted(adminApplicationComparator(sorting)).toList();
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(page, 0);
        int from = (int) Math.min((long) safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        return CursorPageResponse.ofOffset(rows.subList(from, to), to < rows.size(), rows.size(), safePage, safeSize);
    }

    /** 운영진이 자율스터디 신청을 수동 합격 처리한다. */
    @Transactional
    public void acceptAutonomousStudyApplications(Integer studyId, List<Long> applyIds) {
        Study study = getAutonomousStudyForAdminDecision(studyId);

        for (Long applyId : applyIds) {
            Optional<UserApply> applyOpt = findApplication(applyId);
            if (applyOpt.isEmpty()) {
                continue;
            }
            UserApply apply = applyOpt.get();
            requireAutonomousPrimaryApplication(apply, studyId);

            if (apply.getPrimaryStatus() == UserApplyStatus.ACCEPT) {
                continue;
            }
            apply.updateStatus(studyId, UserApplyStatus.ACCEPT);
            duesService.ensureMemberCheck(study, apply.getApplier());
            duesService.registerStudyUserIfEligible(study, apply.getApplier());
        }
    }

    /** 운영진이 자율스터디 신청을 수동 불합격 처리한다. */
    @Transactional
    public void rejectAutonomousStudyApplications(Integer studyId, List<Long> applyIds) {
        Study study = getAutonomousStudyForAdminDecision(studyId);

        for (Long applyId : applyIds) {
            Optional<UserApply> applyOpt = findApplication(applyId);
            if (applyOpt.isEmpty()) {
                continue;
            }
            UserApply apply = applyOpt.get();
            requireAutonomousPrimaryApplication(apply, studyId);

            if (apply.getPrimaryStatus() == UserApplyStatus.REJECT) {
                continue;
            }
            if (apply.getPrimaryStatus() == UserApplyStatus.ACCEPT) {
                removeRevertedAcceptanceMembership(study, studyId, apply);
            }
            apply.updateStatus(studyId, UserApplyStatus.REJECT);
        }
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private Comparator<AdminStudyApplicationInfo> adminApplicationComparator(List<SortCriteria> sorting) {
        if (sorting == null || sorting.isEmpty()) {
            return Comparator.comparing(AdminStudyApplicationInfo::appliedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(AdminStudyApplicationInfo::userId)
                    .thenComparing(AdminStudyApplicationInfo::priority);
        }

        Comparator<AdminStudyApplicationInfo> result = null;
        for (SortCriteria criteria : sorting) {
            Comparator<AdminStudyApplicationInfo> next = switch (criteria.field()) {
                case "userId" -> Comparator.comparing(AdminStudyApplicationInfo::userId);
                case "userName" -> Comparator.comparing(AdminStudyApplicationInfo::userName,
                        Comparator.nullsLast(String::compareTo));
                case "department" -> Comparator.comparing(AdminStudyApplicationInfo::department,
                        Comparator.nullsLast(String::compareTo));
                case "studyName" -> Comparator.comparing(AdminStudyApplicationInfo::studyName,
                        Comparator.nullsLast(String::compareTo));
                case "priority" -> Comparator.comparing(AdminStudyApplicationInfo::priority);
                case "appliedAt" -> Comparator.comparing(AdminStudyApplicationInfo::appliedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                default -> throw new ForifException(ErrorCode.INVALID_INPUT);
            };
            if (criteria.direction() == SortDirection.DESC) {
                next = next.reversed();
            }
            result = result == null ? next : result.thenComparing(next);
        }
        return result.thenComparing(AdminStudyApplicationInfo::userId)
                .thenComparing(AdminStudyApplicationInfo::priority);
    }

    /**
     * 본인의 활동 학기 대기 중인 특정 우선순위 스터디 신청을 멘티 모집 종료 전까지 취소합니다.
     * 1순위 취소 시 2순위가 있으면 1순위로 승격하며, 2순위 취소 시 1순위는 유지합니다.
     */
    @Transactional
    public void cancelApplication(Long userId, Long applyId, int priority) {
        UserApply apply = getApplication(applyId);

        if (!apply.getApplier().getId().equals(userId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        requireActiveSemesterApplication(apply);
        semesterPhaseGuard.requireNotEnded(
                SemesterPhase.MENTEE_RECRUIT, apply.getApplyYear(), apply.getApplySemester());

        if (priority == 1) {
            if (apply.getPrimaryStatus() != UserApplyStatus.PENDING) {
                throw new ForifException(ErrorCode.APPLY_NOT_PENDING);
            }

            // 불합격한 2순위를 1순위로 올리면 지원자가 취소도 재지원도 못 하는 상태로 갇힌다.
            // 살릴 값이 없으므로 신청서 자체를 지워 다시 지원할 수 있게 둔다.
            boolean hasPromotableSecondary = apply.getSecondaryStudy() != null
                    && apply.getSecondaryStatus() != UserApplyStatus.REJECT;

            if (hasPromotableSecondary) {
                apply.promoteSecondaryToPrimary();
            } else {
                userRepository.deleteUserApply(apply);
            }
            return;
        }

        if (priority == 2) {
            if (apply.getSecondaryStudy() == null) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
            if (apply.getSecondaryStatus() != UserApplyStatus.PENDING) {
                throw new ForifException(ErrorCode.APPLY_NOT_PENDING);
            }

            apply.cancelSecondaryApplication();
            return;
        }

        throw new ForifException(ErrorCode.INVALID_INPUT);
    }

    private UserApply getApplication(Long applyId) {
        return findApplication(applyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_APPLY_NOT_FOUND));
    }

    private Optional<UserApply> findApplication(Long applyId) {
        return userRepository.findUserApplyById(applyId);
    }

    /**
     * 합격 처리 메서드
     * @param userId 멘토 유저 id
     * @param studyId 스터디 id
     * @param applyIds 합격 처리할 신청서 id 목록
     */
    @Transactional
    public void acceptApplications(Long userId, Integer studyId, List<Long> applyIds) {
        Study study = getStudyIfActiveMentor(userId, studyId);
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_REVIEW);

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
        Study study = getStudyIfActiveMentor(userId, studyId);
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_REVIEW);

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
                removeRevertedAcceptanceMembership(study, studyId, apply);
            }

            apply.updateStatus(studyId, UserApplyStatus.REJECT);
        }
    }

    /**
     * 현재 유저의 이번 학기 지원 상태 반환
     * @param userId 유저 id
     * @return 지원 상태 응답
     */
    @Transactional(readOnly = true)
    public ApplyStatusInfo getApplyStatus(Long userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        SemesterInfo active = semesterService.getActive();
        int year = active.actYear();
        int semester = active.actSemester();
        boolean menteeRecruitmentOpen = semesterPhaseGuard.isOpen(
                SemesterPhase.MENTEE_RECRUIT, year, semester);

        Optional<UserApply> applyOpt = userRepository.findUserApplyByYearAndSemesterAndUser(year, semester, user);

        if (applyOpt.isEmpty()) {
            return ApplyStatusInfo.builder()
                    .canApplyPrimary(menteeRecruitmentOpen)
                    .canApplySecondary(false)
                    .canApplyAutonomousStudy(menteeRecruitmentOpen)
                    .hasAutonomousStudyApplication(false)
                    .build();
        }

        UserApply apply = applyOpt.get();
        boolean hasSecondary = apply.getSecondaryStudy() != null;

        Study primaryStudy = studyRepository.findStudyByIdWithTags(apply.getPrimaryStudy())
                .orElse(null);
        boolean isAutonomousApplication = primaryStudy != null && primaryStudy.isAutonomousStudy();

        StudyDto secondaryStudyDto = hasSecondary
                ? studyRepository.findStudyByIdWithTags(apply.getSecondaryStudy())
                        .map(StudyDto::from)
                        .orElse(null)
                : null;

        return ApplyStatusInfo.builder()
                .canApplyPrimary(false)
                .canApplySecondary(menteeRecruitmentOpen && !hasSecondary && !isAutonomousApplication)
                .canApplyAutonomousStudy(false)
                .hasAutonomousStudyApplication(isAutonomousApplication)
                .primaryStudy(primaryStudy == null ? null : StudyDto.from(primaryStudy))
                .secondaryStudy(secondaryStudyDto)
                .build();
    }

    /**
     * 지원자 목록을 조회하는 메서드입니다.
     */
    @Transactional(readOnly = true)
    public Page<UserApplyInfo> getApplyInfo(Long userId, Integer studyId, int page, int pageSize, UserApplyStatus statusFilter,
                                                          SortDirection applyDateDirection) {
        Study study = getStudyIfMentor(userId, studyId);
        Pageable pageable = PageRequest.of(page, pageSize);
        return userRepository.findUserApply(study.getId(), pageable, statusFilter, applyDateDirection).map(apply -> UserApplyInfo.from(apply, study));
    }

    /**
     * 지원 내역 상세 조회 메서드입니다.
     */
    @Transactional(readOnly = true)
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
    public void updateApplyStatus(Long userId, Integer studyId, Long applyId, UserApplyStatus newStatus) {
        Study study = getStudyIfActiveMentor(userId, studyId);
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_REVIEW);
        UserApply userApply = getApplication(applyId);

        // 신청서가 해당 스터디에 대한 것인지 검증
        if (userApply.getPrimaryStudy() != study.getId() && !study.getId().equals(userApply.getSecondaryStudy())) {
            throw new ForifException(ErrorCode.USER_NOT_APPLIED_TO_STUDY);
        }

        // ACCEPT는 /accept 엔드포인트를 사용해야 함 (StudyUser 동기화 필요)
        if (newStatus == UserApplyStatus.ACCEPT) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        UserApplyStatus currentStatus = userApply.getPrimaryStudy() == studyId
                ? userApply.getPrimaryStatus()
                : userApply.getSecondaryStatus();
        if (currentStatus == UserApplyStatus.ACCEPT) {
            removeRevertedAcceptanceMembership(study, studyId, userApply);
        }
        userApply.updateStatus(studyId, newStatus);
    }

    /** 조회용. 지난 학기 스터디도 본인이 멘토였으면 볼 수 있다. */
    private Study getStudyIfMentor(Long userId, Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        studyMentorAccess.requireMentor(study, userId);
        requireApplicantManagementTarget(study);
        return study;
    }

    /** 변경용. 활동 학기 스터디만 건드릴 수 있다. */
    private Study getStudyIfActiveMentor(Long userId, Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        if (study.isAutonomousStudy()) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_APPLICATION_DECISION_NOT_ALLOWED);
        }
        studyMentorAccess.requireMentorOfActiveSemester(study, userId);
        if (study.getStudyStatus() != StudyStatus.APPROVED) {
            throw new ForifException(ErrorCode.BAD_REQUEST);
        }
        return study;
    }

    private Study getAutonomousStudyForAdminDecision(Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));
        if (!study.isAutonomousStudy()) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        SemesterInfo active = semesterService.getActive();
        if (!active.matches(study.getActYear(), study.getActSemester())) {
            throw new ForifException(ErrorCode.STUDY_NOT_IN_ACTIVE_SEMESTER);
        }
        if (study.getStudyStatus() != StudyStatus.APPROVED) {
            throw new ForifException(ErrorCode.BAD_REQUEST);
        }
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_REVIEW);
        return study;
    }

    private void requireAutonomousPrimaryApplication(UserApply apply, Integer studyId) {
        if (apply.getPrimaryStudy() != studyId) {
            throw new ForifException(ErrorCode.USER_NOT_APPLIED_TO_STUDY);
        }
    }

    /**
     * 멘토/운영진이 심사 중 합격을 번복한 경우의 정리다.
     * 부원 명단 삭제와 달리, 이 경우는 심사 불합격으로 다시 수신자 목록에 포함되어야 하므로
     * 합격 확인 기록도 함께 지운다.
     */
    private void removeRevertedAcceptanceMembership(Study study, Integer studyId, UserApply apply) {
        Long userId = apply.getApplier().getId();
        studyUserRepository.deleteByUserIdAndStudyId(userId, studyId);
        memberSemesterCheckRepository.deleteByUserIdAndYearSemester(
                userId, study.getActYear(), study.getActSemester());
    }

    /** 신청자 이력은 승인·개설 스터디에서만 조회한다. */
    private void requireApplicantManagementTarget(Study study) {
        if (study.getStudyStatus() != StudyStatus.APPROVED
                && study.getStudyStatus() != StudyStatus.STARTED) {
            throw new ForifException(ErrorCode.BAD_REQUEST);
        }
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
            UserApplyCommand request,
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

    private void requireRegularStudyApplicationInput(UserApplyCommand request) {
        if (request.priority() == null
                || request.applyReason() == null
                || request.applyReason().isBlank()) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateRegularStudyApplicationUpdateInput(UserApplyUpdateCommand request) {
        List<ApiErrorData> errors = validator.validate(request).stream()
                .map(this::toApiErrorData)
                .toList();

        if (!errors.isEmpty()) {
            throw new ForifException(ErrorCode.VALIDATION_FAILED, errors);
        }
    }

    private ApiErrorData toApiErrorData(ConstraintViolation<UserApplyUpdateCommand> violation) {
        return new ApiErrorData(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue()
        );
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
