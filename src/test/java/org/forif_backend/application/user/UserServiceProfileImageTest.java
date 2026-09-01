package org.forif_backend.application.user;

import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.GoogleOAuthClient;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileImageTest {

    private static final Long USER_ID = 2024000000L;
    private static final String PREVIOUS_OBJECT_KEY = "users/profiles/previous.png";
    private static final String UPLOADED_OBJECT_KEY = "users/profiles/uploaded.png";

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

    @InjectMocks
    private UserService userService;

    private User user;
    private MockMultipartFile profileImage;

    @BeforeEach
    void setUp() {
        user = User.createUser(
                USER_ID,
                "테스트 사용자",
                "test@hanyang.ac.kr",
                "010-0000-0000",
                "컴퓨터학부"
        );
        user.updateProfile(null, PREVIOUS_OBJECT_KEY);
        profileImage = new MockMultipartFile(
                "profileImage",
                "profile.png",
                "image/png",
                new byte[]{1}
        );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(filePort.uploadFile(profileImage, "users/profiles")).thenReturn(UPLOADED_OBJECT_KEY);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletesPreviousProfileImageAfterCommit() {
        userService.updateUserProfile(USER_ID, "컴퓨터학부", profileImage);

        assertEquals(UPLOADED_OBJECT_KEY, user.getImgUrl());
        verify(filePort, never()).deleteFile(PREVIOUS_OBJECT_KEY);

        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(filePort).deleteFile(PREVIOUS_OBJECT_KEY);
        verify(filePort, never()).deleteFile(UPLOADED_OBJECT_KEY);
    }

    @Test
    void deletesUploadedProfileImageAfterRollback() {
        userService.updateUserProfile(USER_ID, "컴퓨터학부", profileImage);

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(filePort).deleteFile(UPLOADED_OBJECT_KEY);
        verify(filePort, never()).deleteFile(PREVIOUS_OBJECT_KEY);
    }

    private void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(status));
    }
}
