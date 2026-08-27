package org.forif_backend.application.user;

import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.study.StudyMentorAccess;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplyServiceAcceptanceTest {

    @Mock
    private SemesterService semesterService;
    @Mock
    private SemesterPhaseGuard semesterPhaseGuard;
    @Mock
    private StudyMentorAccess studyMentorAccess;
    @Mock
    private DuesService duesService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserApplyRepository userApplyRepository;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private StudyUserRepository studyUserRepository;

    @InjectMocks
    private UserApplyService userApplyService;

    @Test
    void acceptsApplicationWithoutDirectlyCreatingStudyUser() {
        Study study = mock(Study.class);
        User applicant = User.createUser(1L, "신청자", "applicant@hanyang.ac.kr", "01011112222", "컴퓨터학부");
        UserApply application = mock(UserApply.class);

        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(userRepository.findUserApplyById(100L)).thenReturn(Optional.of(application));
        when(application.getPrimaryStudy()).thenReturn(10);
        when(application.getApplier()).thenReturn(applicant);

        userApplyService.acceptApplications(99L, 10, List.of(100L));

        verify(semesterPhaseGuard).requireOpen(SemesterPhase.MENTEE_REVIEW);
        verify(application).updateStatus(10, org.forif_backend.domain.user.UserApplyStatus.ACCEPT);
        verify(duesService).ensureMemberCheck(study, applicant);
        verify(duesService).registerStudyUserIfEligible(study, applicant);
        verify(studyUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsDeletedApplicationAndAcceptsRemainingApplications() {
        Study study = mock(Study.class);
        User applicant = User.createUser(1L, "신청자", "applicant@hanyang.ac.kr", "01011112222", "컴퓨터학부");
        UserApply remainingApplication = mock(UserApply.class);

        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(userRepository.findUserApplyById(100L)).thenReturn(Optional.empty());
        when(userRepository.findUserApplyById(101L)).thenReturn(Optional.of(remainingApplication));
        when(remainingApplication.getPrimaryStudy()).thenReturn(10);
        when(remainingApplication.getApplier()).thenReturn(applicant);

        userApplyService.acceptApplications(99L, 10, List.of(100L, 101L));

        verify(semesterPhaseGuard).requireOpen(SemesterPhase.MENTEE_REVIEW);
        verify(remainingApplication).updateStatus(10, org.forif_backend.domain.user.UserApplyStatus.ACCEPT);
        verify(duesService).registerStudyUserIfEligible(study, applicant);
    }

    @Test
    void doesNotAcceptApplicationsWhenMenteeReviewIsClosed() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        doThrow(new ForifException(ErrorCode.SEMESTER_PHASE_CLOSED))
                .when(semesterPhaseGuard)
                .requireOpen(SemesterPhase.MENTEE_REVIEW);

        assertThatThrownBy(() -> userApplyService.acceptApplications(99L, 10, List.of(100L)))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_PHASE_CLOSED));

        verify(userRepository, never()).findUserApplyById(anyLong());
        verifyNoInteractions(duesService, studyUserRepository);
    }

    @Test
    void acceptsPendingAutonomousStudyApplicationsWithTheSameMembershipSynchronization() {
        Study autonomousStudy = mock(Study.class);
        User applicant = User.createUser(1L, "신청자", "applicant@hanyang.ac.kr", "01011112222", "컴퓨터학부");
        UserApply application = mock(UserApply.class);
        when(autonomousStudy.isAutonomousStudy()).thenReturn(true);
        when(autonomousStudy.getId()).thenReturn(10);
        when(userApplyRepository.findAllByYearSemester(2026, 2))
                .thenReturn(List.of(application));
        when(application.getPrimaryStudy()).thenReturn(10);
        when(application.getPrimaryStatus()).thenReturn(UserApplyStatus.PENDING);
        when(application.getApplier()).thenReturn(applicant);

        int acceptedCount = userApplyService.acceptPendingAutonomousStudyApplications(autonomousStudy, 2026, 2);

        org.assertj.core.api.Assertions.assertThat(acceptedCount).isEqualTo(1);
        verify(application).updateStatus(10, UserApplyStatus.ACCEPT);
        verify(duesService).ensureMemberCheck(autonomousStudy, applicant);
        verify(duesService).registerStudyUserIfEligible(autonomousStudy, applicant);
    }

    @Test
    void rejectsManualAcceptanceForAnAutonomousStudy() {
        Study autonomousStudy = mock(Study.class);
        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(autonomousStudy));
        when(autonomousStudy.isAutonomousStudy()).thenReturn(true);

        assertThatThrownBy(() -> userApplyService.acceptApplications(99L, 10, List.of(100L)))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_APPLICATION_DECISION_NOT_ALLOWED));

        verifyNoInteractions(semesterPhaseGuard, userRepository, duesService, studyUserRepository);
    }

    @Test
    void rejectsAutomaticAcceptanceForARegularStudy() {
        Study regularStudy = mock(Study.class);
        when(regularStudy.isAutonomousStudy()).thenReturn(false);

        assertThatThrownBy(() -> userApplyService.acceptPendingAutonomousStudyApplications(regularStudy, 2026, 2))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));

        verifyNoInteractions(userApplyRepository, duesService);
    }
}
