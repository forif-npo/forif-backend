package org.forif_backend.application.staff;

import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 회장단의 비밀번호 재설정도 기존 세션을 끊어야 한다.
 * 재설정은 대개 유출 대응인데, refresh 토큰이 살아 있으면
 * 탈취범이 30일 동안 로테이션으로 접근을 유지한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffAccountServicePasswordResetTest {

    private static final Long PRESIDENT_ID = 20260001L;
    private static final Long TARGET_ID = 20260002L;
    private static final String NEW_PASSWORD = "ResetPass1!";

    @Mock private SemesterService semesterService;
    @Mock private StaffAccountRepository staffAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private ForifTeamRepository forifTeamRepository;
    @InjectMocks private StaffAccountService staffAccountService;

    private StaffAccount president() {
        StaffAccount account = mock(StaffAccount.class);
        when(account.getUserId()).thenReturn(PRESIDENT_ID);
        when(account.getAffiliation()).thenReturn("회장");
        return account;
    }

    private StaffAccount targetAdmin() {
        StaffAccount account = mock(StaffAccount.class);
        when(account.getAffiliation()).thenReturn("운영진");
        return account;
    }

    @Test
    void 운영진_비밀번호_재설정은_대상자의_ADMIN_세션을_무효화한다() {
        StaffAccount president = president();
        StaffAccount target = targetAdmin();
        when(staffAccountRepository.findByUserIdAndRole(PRESIDENT_ID, StaffRole.ADMIN))
                .thenReturn(Optional.of(president));
        when(staffAccountRepository.findByUserIdAndRole(TARGET_ID, StaffRole.ADMIN))
                .thenReturn(Optional.of(target));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("encoded");

        staffAccountService.updateAdmin(PRESIDENT_ID, TARGET_ID, null, NEW_PASSWORD, null);

        verify(refreshTokenService).deleteRefreshToken(TARGET_ID.toString(), StaffRole.ADMIN.getValue());
    }

    @Test
    void 비밀번호를_바꾸지_않는_수정은_세션을_건드리지_않는다() {
        StaffAccount president = president();
        StaffAccount target = targetAdmin();
        when(staffAccountRepository.findByUserIdAndRole(PRESIDENT_ID, StaffRole.ADMIN))
                .thenReturn(Optional.of(president));
        when(staffAccountRepository.findByUserIdAndRole(TARGET_ID, StaffRole.ADMIN))
                .thenReturn(Optional.of(target));

        staffAccountService.updateAdmin(PRESIDENT_ID, TARGET_ID, "새이름", null, null);

        verify(refreshTokenService, never()).deleteRefreshToken(TARGET_ID.toString(), StaffRole.ADMIN.getValue());
    }

}
