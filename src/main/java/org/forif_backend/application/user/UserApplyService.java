package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.dto.ApplyDetailInfo;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.semester.SemesterPhase;
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

        if (request.priority() == 1) {
            if (userRepository.existUserApply(year, semester, user)) {
                throw new ForifException(ErrorCode.ALREADY_APPLIED_PRIMARY);
            }
            UserApply userApply = UserApply.applyStudy(user, study, request.applyReason(), year, semester);
            userRepository.createUserApply(userApply);
        } else if (request.priority() == 2) {
            UserApply existingApply = userRepository.findUserApplyByYearAndSemesterAndUser(year, semester, user)
                    .orElseThrow(() -> new ForifException(ErrorCode.PRIMARY_NOT_APPLIED));

            if (existingApply.getSecondaryStudy() != null) {
                throw new ForifException(ErrorCode.ALREADY_APPLIED_SECONDARY);
            }

            existingApply.addSecondaryStudy(study.getId(), study.getStudyName(), request.applyReason());
        } else {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 스터디 수강 신청서 수정 메서드
     * PENDING 상태인 경우에만 스터디 변경 및 지원 동기 수정이 가능합니다.
     * @param userId 유저 id
     * @param applyId 신청서 id
     * @param request 수정 요청 dto
     */
    @Transactional
    public void updateApplication(Long userId, Long applyId, UserApplyUpdateRequest request) {
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_RECRUIT);

        UserApply apply = userRepository.findUserApplyById(applyId);

        if (!apply.getApplier().getId().equals(userId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        Study study = studyRepository.findStudyById(request.studyId())
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

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
     * 합격 처리 메서드
     * @param userId 멘토 유저 id
     * @param studyId 스터디 id
     * @param applyIds 합격 처리할 신청서 id 목록
     */
    @Transactional
    public void acceptApplications(Long userId, Integer studyId, List<Long> applyIds) {
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTEE_REVIEW);

        Study study = getStudyIfMentor(userId, studyId);

        for (Long applyId : applyIds) {
            UserApply apply = userRepository.findUserApplyById(applyId);

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

            StudyUser studyUser = StudyUser.create(study, apply.getApplier());
            studyUserRepository.save(studyUser);
            duesService.ensureMemberCheck(study, apply.getApplier());
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

        getStudyIfMentor(userId, studyId);

        for (Long applyId : applyIds) {
            UserApply apply = userRepository.findUserApplyById(applyId);

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

        Optional<UserApply> applyOpt = userRepository.findUserApplyByYearAndSemesterAndUser(year, semester, user);

        if (applyOpt.isEmpty()) {
            return ApplyStatusResponse.builder()
                    .canApplyPrimary(true)
                    .canApplySecondary(false)
                    .build();
        }

        UserApply apply = applyOpt.get();
        boolean hasSecondary = apply.getSecondaryStudy() != null;

        StudyResponse primaryStudyResponse = studyRepository.findStudyByIdWithTags(apply.getPrimaryStudy())
                .map(s -> StudyResponse.from(StudyDto.from(s)))
                .orElse(null);

        StudyResponse secondaryStudyResponse = hasSecondary
                ? studyRepository.findStudyByIdWithTags(apply.getSecondaryStudy())
                        .map(s -> StudyResponse.from(StudyDto.from(s)))
                        .orElse(null)
                : null;

        return ApplyStatusResponse.builder()
                .canApplyPrimary(false)
                .canApplySecondary(!hasSecondary)
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
        UserApply userApply = userRepository.findUserApplyById(applyId);

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

        Study study = getStudyIfMentor(userId, studyId);
        UserApply userApply = userRepository.findUserApplyById(applyId);

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
