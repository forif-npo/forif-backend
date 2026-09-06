package org.forif_backend.application.user;

import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.GoogleOAuthClient;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    private StudyRepository studyRepository;
    @Mock
    private StudyUserRepository studyUserRepository;
    @Mock
    private DuesService duesService;
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
    @InjectMocks
    private UserService userService;

    @Test
    void delegatesCurrentSemesterMemberDeletionToRegistrationWithdrawal() {
        SemesterInfo activeSemester = SemesterInfo.of(2026, 2);
        when(semesterService.getActive()).thenReturn(activeSemester);

        userService.deleteCurrentSemesterMember(USER_ID);

        verify(duesService).withdrawCurrentSemesterRegistrations(List.of(USER_ID));
    }
}
