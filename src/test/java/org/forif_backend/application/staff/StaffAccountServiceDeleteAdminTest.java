package org.forif_backend.application.staff;

import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceDeleteAdminTest {

    private static final Long REQUESTER_ID = 20260001L;
    private static final Long TARGET_ID = 20260002L;

    @Mock
    private SemesterService semesterService;
    @Mock
    private StaffAccountRepository staffAccountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ForifTeamRepository forifTeamRepository;
    @InjectMocks
    private StaffAccountService staffAccountService;

    @Test
    void deletesAdminAccountAndInvalidatesItsAdminRefreshToken() {
        StaffAccount requester = adminAccount(REQUESTER_ID, "회장");
        StaffAccount target = adminAccount(TARGET_ID, "운영진");
        when(staffAccountRepository.findByUserIdAndRole(REQUESTER_ID, StaffRole.ADMIN))
                .thenReturn(Optional.of(requester));
        when(staffAccountRepository.findByUserIdAndRole(TARGET_ID, StaffRole.ADMIN))
                .thenReturn(Optional.of(target));

        staffAccountService.deleteAdmin(REQUESTER_ID, TARGET_ID);

        verify(staffAccountRepository).delete(target);
        verify(refreshTokenService).deleteRefreshToken(TARGET_ID.toString(), StaffRole.ADMIN.getValue());
    }

    @Test
    void vicePresidentCannotDeletePresident() {
        StaffAccount requester = adminAccount(REQUESTER_ID, "부회장");
        StaffAccount target = adminAccount(TARGET_ID, "회장");
        when(staffAccountRepository.findByUserIdAndRole(REQUESTER_ID, StaffRole.ADMIN))
                .thenReturn(Optional.of(requester));
        when(staffAccountRepository.findByUserIdAndRole(TARGET_ID, StaffRole.ADMIN))
                .thenReturn(Optional.of(target));

        ForifException exception = assertThrows(
                ForifException.class,
                () -> staffAccountService.deleteAdmin(REQUESTER_ID, TARGET_ID));

        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.getErrorCode());
        verify(staffAccountRepository, never()).delete(target);
    }

    private StaffAccount adminAccount(Long userId, String affiliation) {
        User user = User.createUser(userId, "운영진", userId + "@hanyang.ac.kr", "010-0000-0000", "컴퓨터학부");
        return StaffAccount.createStaffAccount(user, "encoded-password", "운영진", StaffRole.ADMIN, affiliation);
    }
}
