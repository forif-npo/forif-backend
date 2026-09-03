package org.forif_backend.application.user;

import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.study.StudyMentorAccess;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplyServiceDeletedApplicationTest {

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
    private StudyRepository studyRepository;
    @Mock
    private StudyUserRepository studyUserRepository;

    @InjectMocks
    private UserApplyService userApplyService;

    @Test
    void skipsDeletedApplicationAndRejectsRemainingApplications() {
        Study study = mock(Study.class);
        UserApply remainingApplication = mock(UserApply.class);

        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(userRepository.findUserApplyById(100L)).thenReturn(Optional.empty());
        when(userRepository.findUserApplyById(101L)).thenReturn(Optional.of(remainingApplication));
        when(remainingApplication.getPrimaryStudy()).thenReturn(10);

        userApplyService.rejectApplications(99L, 10, List.of(100L, 101L));

        verify(semesterPhaseGuard).requireOpenForUpdate(SemesterPhase.MENTEE_REVIEW);
        verify(remainingApplication).updateStatus(10, UserApplyStatus.REJECT);
    }

    @Test
    void returnsNotFoundWhenMentorViewsDeletedApplication() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(userRepository.findUserApplyById(100L)).thenReturn(Optional.empty());

        assertError(() -> userApplyService.getApplyDetailInfo(99L, 10, 100L));
    }

    @Test
    void returnsNotFoundWhenMentorUpdatesDeletedApplication() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(userRepository.findUserApplyById(100L)).thenReturn(Optional.empty());

        assertError(() -> userApplyService.updateApplyStatus(
                99L, 10, 100L, UserApplyStatus.REJECT));

        verify(semesterPhaseGuard).requireOpenForUpdate(SemesterPhase.MENTEE_REVIEW);
    }

    @Test
    void doesNotRejectOrUpdateStatusWhenMenteeReviewIsClosed() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        doThrow(new ForifException(ErrorCode.SEMESTER_PHASE_CLOSED))
                .when(semesterPhaseGuard)
                .requireOpenForUpdate(SemesterPhase.MENTEE_REVIEW);

        assertPhaseClosed(() -> userApplyService.rejectApplications(99L, 10, List.of(100L)));
        assertPhaseClosed(() -> userApplyService.updateApplyStatus(
                99L, 10, 100L, UserApplyStatus.REJECT));

        verify(userRepository, never()).findUserApplyById(anyLong());
    }

    private void assertError(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.STUDY_APPLY_NOT_FOUND));
    }

    private void assertPhaseClosed(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_PHASE_CLOSED));
    }
}
