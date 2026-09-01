package org.forif_backend.application.staff;

import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.staff.dto.CreateAdminCommand;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.study.StudyRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAccountServicePasswordChangeTest {

    private static final Long USER_ID = 20260001L;
    private static final String CURRENT_PASSWORD = "Current12!";
    private static final String NEW_PASSWORD = "NewPassword1!";

    @Mock private SemesterService semesterService;
    @Mock private StaffAccountRepository staffAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private ForifTeamRepository forifTeamRepository;
    @InjectMocks private StaffAccountService staffAccountService;

    @Test
    void changesPasswordAfterVerifyingCurrentPasswordAndInvalidatesAdminRefreshToken() {
        StaffAccount account = adminAccount("encoded-current-password");
        when(staffAccountRepository.findByUserIdAndRole(USER_ID, StaffRole.ADMIN)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "encoded-current-password")).thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, "encoded-current-password")).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("encoded-new-password");

        staffAccountService.changeAdminPassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

        assertThat(account.getPassword()).isEqualTo("encoded-new-password");
        verify(refreshTokenService).deleteRefreshToken(USER_ID.toString(), StaffRole.ADMIN.getValue());
    }

    @Test
    void rejectsPasswordChangeWhenCurrentPasswordDoesNotMatch() {
        StaffAccount account = adminAccount("encoded-current-password");
        when(staffAccountRepository.findByUserIdAndRole(USER_ID, StaffRole.ADMIN)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "encoded-current-password")).thenReturn(false);

        assertThatThrownBy(() -> staffAccountService.changeAdminPassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(ForifException.class)
                .extracting(exception -> ((ForifException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

        verify(passwordEncoder, never()).encode(NEW_PASSWORD);
        verify(refreshTokenService, never()).deleteRefreshToken(USER_ID.toString(), StaffRole.ADMIN.getValue());
    }

    @Test
    void rejectsNewPasswordThatDoesNotMeetPasswordPolicy() {
        StaffAccount account = adminAccount("encoded-current-password");
        when(staffAccountRepository.findByUserIdAndRole(USER_ID, StaffRole.ADMIN)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "encoded-current-password")).thenReturn(true);

        assertThatThrownBy(() -> staffAccountService.changeAdminPassword(USER_ID, CURRENT_PASSWORD, "short1!"))
                .isInstanceOf(ForifException.class)
                .extracting(exception -> ((ForifException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(passwordEncoder, never()).encode("short1!");
        verify(refreshTokenService, never()).deleteRefreshToken(USER_ID.toString(), StaffRole.ADMIN.getValue());
    }

    @Test
    void rejectsInvalidPasswordBeforeCreatingAdminAccount() {
        StaffAccount requester = adminAccount("encoded-requester-password");
        when(staffAccountRepository.findByUserIdAndRole(USER_ID, StaffRole.ADMIN)).thenReturn(Optional.of(requester));
        when(staffAccountRepository.existsByUserIdAndRole(20260002L, StaffRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> staffAccountService.createAdmin(
                USER_ID,
                new CreateAdminCommand(20260002L, "onlylowercase", "기획팀")
        )).isInstanceOf(ForifException.class)
                .extracting(exception -> ((ForifException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(userRepository, never()).findById(20260002L);
    }

    private StaffAccount adminAccount(String password) {
        User user = User.createUser(USER_ID, "운영진", "admin@hanyang.ac.kr", "010-0000-0000", "컴퓨터학부");
        return StaffAccount.createStaffAccount(user, password, "운영진", StaffRole.ADMIN, "회장");
    }
}
