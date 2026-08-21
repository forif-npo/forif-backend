package org.forif_backend.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.study.StudyMentorAccess;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.userApply.dto.ApplyStatusResponse;
import org.forif_backend.web.userApply.dto.UserApplyRequest;
import org.forif_backend.web.userApply.dto.UserApplyUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserApplyServiceApplyTest {

    private static final long USER_ID = 1L;
    private static final int STUDY_ID = 100;

    @Mock private SemesterService semesterService;
    @Mock private SemesterPhaseGuard semesterPhaseGuard;
    @Mock private StudyMentorAccess studyMentorAccess;
    @Mock private DuesService duesService;
    @Mock private UserRepository userRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private StudyUserRepository studyUserRepository;

    @InjectMocks private UserApplyService userApplyService;

    private final User user = User.createUser(USER_ID, "신청자", "applicant@forif.org", "01012345678", "컴퓨터학부");

    @BeforeEach
    void setUp() {
        lenient().when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        lenient().when(userRepository.findUserById(USER_ID)).thenReturn(java.util.Optional.of(user));
    }

    @Test
    void appliesOnlyToAnApplicableApprovedStudyInTheActiveSemester() {
        Study study = applicableStudy(2026, 2, StudyStatus.APPROVED, RecruitStatus.APPLICABLE);
        when(study.getId()).thenReturn(STUDY_ID);
        when(study.getStudyName()).thenReturn("테스트 스터디");
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(study));

        userApplyService.applyStudy(USER_ID, new UserApplyRequest(STUDY_ID, "지원 동기", 1));

        verify(userRepository).createUserApply(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAStudyOutsideTheActiveSemesterEvenWhenItIsMarkedApplicable() {
        Study study = applicableStudy(2026, 1, StudyStatus.APPROVED, RecruitStatus.APPLICABLE);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(study));

        assertPeriodEnded(() -> userApplyService.applyStudy(USER_ID, new UserApplyRequest(STUDY_ID, "지원 동기", 1)));

        verify(userRepository, never()).createUserApply(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAStudyThatIsNotApprovedOrRecruiting() {
        Study pendingStudy = applicableStudy(2026, 2, StudyStatus.PENDING, RecruitStatus.APPLICABLE);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(pendingStudy));

        assertPeriodEnded(() -> userApplyService.applyStudy(USER_ID, new UserApplyRequest(STUDY_ID, "지원 동기", 1)));

        Study closedStudy = applicableStudy(2026, 2, StudyStatus.APPROVED, RecruitStatus.CLOSED);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(closedStudy));

        assertPeriodEnded(() -> userApplyService.applyStudy(USER_ID, new UserApplyRequest(STUDY_ID, "지원 동기", 1)));
        verify(userRepository, never()).createUserApply(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blocksApplicantDecisionsForAStartedStudy() {
        Study study = applicableStudy(2026, 2, StudyStatus.STARTED, RecruitStatus.CLOSED);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(study));

        assertThatThrownBy(() -> userApplyService.acceptApplications(99L, STUDY_ID, java.util.List.of()))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void updatesAnExistingApplicationWithOnlyTheRecruitmentEndGate() {
        Study originalStudy = org.mockito.Mockito.mock(Study.class);
        when(originalStudy.getId()).thenReturn(10);
        when(originalStudy.getStudyName()).thenReturn("기존 스터디");
        UserApply application = UserApply.applyStudy(user, originalStudy, "기존 지원 동기", 2026, 2);
        Study replacementStudy = applicableStudy(2026, 2, StudyStatus.APPROVED, RecruitStatus.APPLICABLE);
        when(replacementStudy.getId()).thenReturn(STUDY_ID);
        when(replacementStudy.getStudyName()).thenReturn("변경 스터디");
        when(userRepository.findUserApplyById(77L)).thenReturn(application);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(replacementStudy));

        userApplyService.updateApplication(
                USER_ID, 77L, new UserApplyUpdateRequest(STUDY_ID, "수정된 지원 동기", 1));

        verify(semesterPhaseGuard).requireNotEnded(SemesterPhase.MENTEE_RECRUIT, 2026, 2);
        verify(semesterPhaseGuard, never()).requireOpen(SemesterPhase.MENTEE_RECRUIT);
        assertThat(application.getPrimaryStudy()).isEqualTo(STUDY_ID);
    }

    @Test
    void rejectsUpdatingAnApplicationToAStudyThatIsNotApplicable() {
        Study originalStudy = org.mockito.Mockito.mock(Study.class);
        when(originalStudy.getId()).thenReturn(10);
        when(originalStudy.getStudyName()).thenReturn("기존 스터디");
        UserApply application = UserApply.applyStudy(user, originalStudy, "기존 지원 동기", 2026, 2);
        Study closedStudy = applicableStudy(2026, 2, StudyStatus.APPROVED, RecruitStatus.CLOSED);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(closedStudy));
        when(userRepository.findUserApplyById(77L)).thenReturn(application);

        assertPeriodEnded(() -> userApplyService.updateApplication(
                USER_ID, 77L, new UserApplyUpdateRequest(STUDY_ID, "수정된 지원 동기", 1)));

        assertThat(application.getPrimaryStudy()).isEqualTo(10);
    }

    @Test
    void returnsNoNewApplicationAvailabilityWhenMenteeRecruitmentIsClosed() {
        when(semesterPhaseGuard.isOpen(SemesterPhase.MENTEE_RECRUIT, 2026, 2)).thenReturn(false);
        when(userRepository.findUserApplyByYearAndSemesterAndUser(2026, 2, user))
                .thenReturn(java.util.Optional.empty());

        ApplyStatusResponse response = userApplyService.getApplyStatus(USER_ID);

        assertThat(response.canApplyPrimary()).isFalse();
        assertThat(response.canApplySecondary()).isFalse();
        assertThat(response.canApplyAutonomousStudy()).isFalse();
        assertThat(response.hasAutonomousStudyApplication()).isFalse();
    }

    @Test
    void reportsAutonomousStudyApplicationAsUnavailableForBothStudyTypes() {
        when(semesterPhaseGuard.isOpen(SemesterPhase.MENTEE_RECRUIT, 2026, 2)).thenReturn(true);

        UserApply application = mock(UserApply.class);
        when(application.getPrimaryStudy()).thenReturn(999);
        when(application.getSecondaryStudy()).thenReturn(null);
        when(userRepository.findUserApplyByYearAndSemesterAndUser(2026, 2, user))
                .thenReturn(java.util.Optional.of(application));

        Study autonomousStudy = mock(Study.class);
        when(autonomousStudy.getId()).thenReturn(999);
        when(autonomousStudy.getTags()).thenReturn(java.util.List.of());
        when(autonomousStudy.isAutonomousStudy()).thenReturn(true);
        when(studyRepository.findStudyByIdWithTags(999))
                .thenReturn(java.util.Optional.of(autonomousStudy));

        ApplyStatusResponse response = userApplyService.getApplyStatus(USER_ID);

        assertThat(response.canApplyPrimary()).isFalse();
        assertThat(response.canApplySecondary()).isFalse();
        assertThat(response.canApplyAutonomousStudy()).isFalse();
        assertThat(response.hasAutonomousStudyApplication()).isTrue();
    }

    @Test
    void appliesToAnAutonomousStudyWithoutPriorityOrApplyReason() {
        Study study = applicableStudy(2026, 2, StudyStatus.APPROVED, RecruitStatus.APPLICABLE);
        when(study.getId()).thenReturn(STUDY_ID);
        when(study.getStudyName()).thenReturn("자율스터디");
        when(study.isAutonomousStudy()).thenReturn(true);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(study));
        when(userRepository.findUserApplyByYearAndSemesterAndUser(2026, 2, user))
                .thenReturn(java.util.Optional.empty());

        userApplyService.applyStudy(USER_ID, new UserApplyRequest(STUDY_ID, null, null));

        verify(userRepository).createUserApply(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAutonomousStudyWhenTheUserAlreadyAppliedToARegularStudy() {
        Study study = applicableStudy(2026, 2, StudyStatus.APPROVED, RecruitStatus.APPLICABLE);
        when(study.isAutonomousStudy()).thenReturn(true);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(study));
        when(userRepository.findUserApplyByYearAndSemesterAndUser(2026, 2, user))
                .thenReturn(java.util.Optional.of(mock(UserApply.class)));

        assertThatThrownBy(() ->
                userApplyService.applyStudy(USER_ID, new UserApplyRequest(STUDY_ID, null, null))
        ).isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_APPLY_CONFLICT));
    }

    @Test
    void rejectsRegularStudyWhenTheUserAlreadyAppliedToAnAutonomousStudy() {
        Study regularStudy = applicableStudy(2026, 2, StudyStatus.APPROVED, RecruitStatus.APPLICABLE);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(java.util.Optional.of(regularStudy));

        UserApply autonomousApplication = mock(UserApply.class);
        when(autonomousApplication.getPrimaryStudy()).thenReturn(999);
        when(userRepository.findUserApplyByYearAndSemesterAndUser(2026, 2, user))
                .thenReturn(java.util.Optional.of(autonomousApplication));

        Study autonomousStudy = mock(Study.class);
        when(autonomousStudy.isAutonomousStudy()).thenReturn(true);
        when(studyRepository.findStudyById(999)).thenReturn(java.util.Optional.of(autonomousStudy));

        assertThatThrownBy(() -> userApplyService.applyStudy(
                USER_ID,
                new UserApplyRequest(
                        STUDY_ID,
                        "정규스터디에서 체계적인 커리큘럼을 따라 학습하고 동료들과 함께 성장하고 싶어 지원합니다.",
                        2
                )
        )).isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_APPLY_CONFLICT));
    }

    private Study applicableStudy(int year, int semester, StudyStatus status, RecruitStatus recruitStatus) {
        Study study = org.mockito.Mockito.mock(Study.class);
        lenient().when(study.getActYear()).thenReturn(year);
        lenient().when(study.getActSemester()).thenReturn(semester);
        lenient().when(study.getStudyStatus()).thenReturn(status);
        lenient().when(study.getRecruitStatus()).thenReturn(recruitStatus);
        return study;
    }

    private void assertPeriodEnded(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.STUDY_APPLICATION_PERIOD_ENDED));
    }
}
