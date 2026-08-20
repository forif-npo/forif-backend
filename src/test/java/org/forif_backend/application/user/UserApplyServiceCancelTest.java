package org.forif_backend.application.user;

import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.study.StudyMentorAccess;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplyServiceCancelTest {

    private static final long APPLICATION_ID = 100L;
    private static final long APPLICANT_ID = 1L;

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

    @BeforeEach
    void setUp() {
        lenient().when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));
    }

    @Test
    void deletesOwnApplicationWhenAllPrioritiesArePending() {
        UserApply application = pendingApplication(applicant());
        application.addSecondaryStudy(20, "2순위 스터디", "2순위 지원 동기");
        when(userRepository.findUserApplyById(APPLICATION_ID)).thenReturn(application);

        userApplyService.cancelApplication(APPLICANT_ID, APPLICATION_ID);

        verify(semesterPhaseGuard).requireNotEnded(SemesterPhase.MENTEE_RECRUIT, 2026, 1);
        verify(userRepository).deleteUserApply(application);
    }

    @Test
    void doesNotDeleteAnotherUsersApplication() {
        UserApply application = pendingApplication(user(2L));
        when(userRepository.findUserApplyById(APPLICATION_ID)).thenReturn(application);

        assertError(ErrorCode.INSUFFICIENT_PERMISSION,
                () -> userApplyService.cancelApplication(APPLICANT_ID, APPLICATION_ID));

        verify(userRepository, never()).deleteUserApply(application);
    }

    @Test
    void doesNotDeleteApplicationWhenPrimaryHasBeenReviewed() {
        UserApply application = pendingApplication(applicant());
        application.updateStatus(10, UserApplyStatus.ACCEPT);
        when(userRepository.findUserApplyById(APPLICATION_ID)).thenReturn(application);

        assertError(ErrorCode.APPLY_NOT_PENDING,
                () -> userApplyService.cancelApplication(APPLICANT_ID, APPLICATION_ID));

        verify(userRepository, never()).deleteUserApply(application);
    }

    @Test
    void doesNotDeleteApplicationWhenSecondaryHasBeenReviewed() {
        UserApply application = pendingApplication(applicant());
        application.addSecondaryStudy(20, "2순위 스터디", "2순위 지원 동기");
        application.updateStatus(20, UserApplyStatus.REJECT);
        when(userRepository.findUserApplyById(APPLICATION_ID)).thenReturn(application);

        assertError(ErrorCode.APPLY_NOT_PENDING,
                () -> userApplyService.cancelApplication(APPLICANT_ID, APPLICATION_ID));

        verify(userRepository, never()).deleteUserApply(application);
    }

    @Test
    void doesNotDeleteApplicationFromAnotherSemester() {
        UserApply application = pendingApplication(applicant(), 2025, 2);
        when(userRepository.findUserApplyById(APPLICATION_ID)).thenReturn(application);

        assertError(ErrorCode.STUDY_APPLY_NOT_IN_ACTIVE_SEMESTER,
                () -> userApplyService.cancelApplication(APPLICANT_ID, APPLICATION_ID));

        verify(userRepository, never()).deleteUserApply(application);
    }

    @Test
    void returnsNotFoundWhenApplicationDoesNotExist() {
        when(userRepository.findUserApplyById(APPLICATION_ID)).thenReturn(null);

        assertError(ErrorCode.STUDY_APPLY_NOT_FOUND,
                () -> userApplyService.cancelApplication(APPLICANT_ID, APPLICATION_ID));

        verify(userRepository, never()).deleteUserApply(any());
    }

    @Test
    void doesNotDeleteApplicationWhenRecruitmentHasEnded() {
        UserApply application = pendingApplication(applicant());
        when(userRepository.findUserApplyById(APPLICATION_ID)).thenReturn(application);
        doThrow(new ForifException(ErrorCode.SEMESTER_PHASE_CLOSED))
                .when(semesterPhaseGuard)
                .requireNotEnded(SemesterPhase.MENTEE_RECRUIT, 2026, 1);

        assertError(ErrorCode.SEMESTER_PHASE_CLOSED,
                () -> userApplyService.cancelApplication(APPLICANT_ID, APPLICATION_ID));

        verify(userRepository, never()).deleteUserApply(any());
    }

    private User applicant() {
        return user(APPLICANT_ID);
    }

    private User user(Long id) {
        return User.createUser(id, "신청자", "applicant@forif.org", "01012345678", "컴퓨터학부");
    }

    private UserApply pendingApplication(User applicant) {
        return pendingApplication(applicant, 2026, 1);
    }

    private UserApply pendingApplication(User applicant, int year, int semester) {
        Study study = mock(Study.class);
        when(study.getId()).thenReturn(10);
        when(study.getStudyName()).thenReturn("1순위 스터디");
        return UserApply.applyStudy(applicant, study, "지원 동기", year, semester);
    }

    private void assertError(ErrorCode expectedErrorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(expectedErrorCode));
    }
}
