package org.forif_backend.application.user;

import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.GoogleOAuthClient;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceMemberDeletionTest {

    private static final Long USER_ID = 20260001L;

    @Mock
    private SemesterService semesterService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserApplyRepository userApplyRepository;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private StudyUserRepository studyUserRepository;
    @Mock
    private StaffAccountRepository staffAccountRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private GoogleOAuthClient googleOAuthClient;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private FilePort filePort;
    @Mock
    private User user;
    @InjectMocks
    private UserService userService;

    @Test
    void deletesOnlyTheCurrentSemesterStudyMemberships() {
        SemesterInfo activeSemester = SemesterInfo.of(2026, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(semesterService.getActive()).thenReturn(activeSemester);
        when(studyUserRepository.deleteByUserIdAndStudyYearSemester(USER_ID, 2026, 2)).thenReturn(2);
        when(userRepository.findUserApplyByYearAndSemesterAndUser(2026, 2, user)).thenReturn(Optional.empty());

        userService.deleteCurrentSemesterMember(USER_ID);

        verify(studyUserRepository).deleteByUserIdAndStudyYearSemester(USER_ID, 2026, 2);
        verify(userRepository, never()).deleteById(USER_ID);
    }

    /**
     * 합격 상태를 남겨두면 회비 확인 시 그 지원서를 근거로 수강생이 되살아나고,
     * 멘토가 다시 합격시켜 복구하는 정상 경로도 막힌다.
     */
    @Test
    void revertsTheAcceptanceSoTheDeletionSticks() {
        Study study = org.mockito.Mockito.mock(Study.class);
        when(study.getId()).thenReturn(100);
        UserApply apply = UserApply.applyStudy(user, study, "지원 사유", 2026, 2);
        apply.updateStatus(100, UserApplyStatus.ACCEPT);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(studyUserRepository.deleteByUserIdAndStudyYearSemester(USER_ID, 2026, 2)).thenReturn(1);
        when(userRepository.findUserApplyByYearAndSemesterAndUser(2026, 2, user)).thenReturn(Optional.of(apply));

        userService.deleteCurrentSemesterMember(USER_ID);

        assertThat(apply.getPrimaryStatus()).isEqualTo(UserApplyStatus.REJECT);
    }

    @Test
    void rejectsDeletionWhenTheUserIsNotInTheCurrentSemester() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(studyUserRepository.deleteByUserIdAndStudyYearSemester(USER_ID, 2026, 2)).thenReturn(0);

        ForifException exception = assertThrows(
                ForifException.class,
                () -> userService.deleteCurrentSemesterMember(USER_ID));

        assertEquals(ErrorCode.CURRENT_SEMESTER_MEMBER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).findUserApplyByYearAndSemesterAndUser(anyInt(), anyInt(), any());
    }

    @Test
    void rejectsDeletionWhenTheUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ForifException exception = assertThrows(
                ForifException.class,
                () -> userService.deleteCurrentSemesterMember(USER_ID));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(studyUserRepository, never()).deleteByUserIdAndStudyYearSemester(any(), anyInt(), anyInt());
    }
}
