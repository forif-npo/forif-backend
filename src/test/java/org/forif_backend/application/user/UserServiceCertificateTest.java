package org.forif_backend.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.user.dto.GetCertificateResult;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.GoogleOAuthClient;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceCertificateTest {

    @Mock private SemesterService semesterService;
    @Mock private UserRepository userRepository;
    @Mock private UserApplyRepository userApplyRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private StudyUserRepository studyUserRepository;
    @Mock private StaffAccountRepository staffAccountRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private GoogleOAuthClient googleOAuthClient;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private FilePort filePort;

    @InjectMocks private UserService userService;

    @Test
    void generatesAFreshViewUrlFromTheStoredCertificateObjectKey() {
        StudyUser studyUser = org.mockito.Mockito.mock(StudyUser.class);
        String objectKey = "certificates/2026-1/1/100.png";
        String viewUrl = "https://example.com/fresh-certificate-url";

        when(studyUserRepository.findByUserIdAndStudyId(1L, 1)).thenReturn(Optional.of(studyUser));
        when(studyUser.getCertificateStatus()).thenReturn(1);
        when(studyUser.getCertificateObjectKey()).thenReturn(objectKey);
        when(filePort.generatePresignedViewUrl(objectKey)).thenReturn(new FileInfo(objectKey, viewUrl));

        GetCertificateResult result = userService.getCertificate(1L, 1);

        assertThat(result.certificateUrl()).isEqualTo(viewUrl);
        verify(filePort).generatePresignedViewUrl(objectKey);
    }

    @Test
    void returnsLegacyStoredUrlWithoutTreatingItAsAnObjectKey() {
        StudyUser studyUser = org.mockito.Mockito.mock(StudyUser.class);
        String legacyUrl = "https://example.com/legacy-certificate-url";

        when(studyUserRepository.findByUserIdAndStudyId(1L, 1)).thenReturn(Optional.of(studyUser));
        when(studyUser.getCertificateStatus()).thenReturn(1);
        when(studyUser.getCertificateObjectKey()).thenReturn(legacyUrl);

        GetCertificateResult result = userService.getCertificate(1L, 1);

        assertThat(result.certificateUrl()).isEqualTo(legacyUrl);
        verify(filePort, never()).generatePresignedViewUrl(legacyUrl);
    }
}
