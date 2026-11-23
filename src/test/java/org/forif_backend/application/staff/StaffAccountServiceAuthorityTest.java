package org.forif_backend.application.staff;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staff.dto.CreateAdminCommand;
import org.forif_backend.application.team.ClubDepartmentService;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.team.ClubDepartment;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceAuthorityTest {

    private static final Long USER_ID = 20260001L;

    @Mock private SemesterService semesterService;
    @Mock private StaffAccountRepository staffAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRepository userRepository;
    @Mock private ForifTeamRepository forifTeamRepository;
    @Mock private ClubDepartmentService clubDepartmentService;
    @Mock private JwtProvider jwtProvider;
    @InjectMocks private StaffAccountService service;

    @Test
    void nextSemesterPresidentHasPresidentAuthorityBeforeTransition() {
        account("운영진");
        upcoming("회장");

        assertEquals("회장", service.getAuthorityAffiliation(USER_ID));
        assertDoesNotThrow(() -> service.requirePresident(USER_ID));
    }

    @Test
    void nextSemesterPresidentCannotDelegateCurrentPresidentOrVicePresident() {
        account("운영진");
        upcoming("회장");

        assertDoesNotThrow(() -> service.requirePresident(USER_ID));
        for (String role : List.of("회장", "부회장")) {
            ForifException denied = assertThrows(ForifException.class,
                    () -> service.delegate(USER_ID, 20260002L, role));
            assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, denied.getErrorCode());
        }
    }

    @Test
    void designatingNextPresidentKeepsIncumbentRoleAndReplacesPreviousDesignation() {
        StaffAccount incumbent = StaffAccount.createStaffAccount(user(), "encoded", "현 회장", StaffRole.ADMIN, "회장");
        Long previousId = 20260002L;
        Long targetId = 20260003L;
        User previousUser = User.createUser(previousId, "이전 차기 회장", "previous@example.com", "010-0000-0000", "컴퓨터학부");
        User targetUser = User.createUser(targetId, "새 차기 회장", "target@example.com", "010-0000-0000", "컴퓨터학부");
        StaffAccount target = StaffAccount.createStaffAccount(targetUser, "encoded", "새 차기 회장", StaffRole.ADMIN, "부회장");
        ForifTeam previous = ForifTeam.create(previousUser, 2027, 1, "기획팀");
        previous.update("회장", null, null, null, null);
        ForifTeam next = ForifTeam.create(targetUser, 2027, 1, "회장단");
        when(staffAccountRepository.findByUserIdAndRole(USER_ID, StaffRole.ADMIN)).thenReturn(Optional.of(incumbent));
        when(staffAccountRepository.findByUserIdAndRole(targetId, StaffRole.ADMIN)).thenReturn(Optional.of(target));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByYearAndSemester(2027, 1)).thenReturn(List.of(previous, next));
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2026, 2, targetId))
                .thenReturn(Optional.empty());
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2027, 1, targetId))
                .thenReturn(Optional.of(next));

        service.delegate(USER_ID, targetId, "회장");

        assertEquals("회장", incumbent.getAffiliation());
        assertEquals("부회장", target.getAffiliation());
        assertEquals("", previous.getUserTitle());
        assertEquals("회장", next.getUserTitle());
        assertEquals("회장", service.getAuthorityAffiliation(targetId));
        StaffAccount previousAccount = StaffAccount.createStaffAccount(
                previousUser, "encoded", "이전 차기 회장", StaffRole.ADMIN, "운영진");
        when(forifTeamRepository.findByYearAndSemester(2026, 2)).thenReturn(List.of());
        var authorities = service.getAdminAuthorities(List.of(previousAccount, target));
        assertNull(authorities.get(previousId).upcomingAffiliation());
        assertEquals("운영진", authorities.get(previousId).affiliation());
        assertEquals("회장", authorities.get(targetId).upcomingAffiliation());
    }

    @Test
    void designatingNextVicePresidentAddsMissingNextSemesterMemberWithoutDemotingIncumbent() {
        StaffAccount incumbent = StaffAccount.createStaffAccount(user(), "encoded", "현 회장", StaffRole.ADMIN, "회장");
        Long viceId = 20260002L;
        StaffAccount currentVice = StaffAccount.createStaffAccount(
                User.createUser(viceId, "현 부회장", "vice@example.com", "010-0000-0000", "컴퓨터학부"),
                "encoded", "현 부회장", StaffRole.ADMIN, "부회장");
        Long targetId = 20260003L;
        User targetUser = User.createUser(targetId, "차기 부회장", "target@example.com", "010-0000-0000", "컴퓨터학부");
        StaffAccount target = StaffAccount.createStaffAccount(targetUser, "encoded", "차기 부회장", StaffRole.ADMIN, "운영진");
        when(staffAccountRepository.findByUserIdAndRole(USER_ID, StaffRole.ADMIN)).thenReturn(Optional.of(incumbent));
        when(staffAccountRepository.findByUserIdAndRole(targetId, StaffRole.ADMIN)).thenReturn(Optional.of(target));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByYearAndSemester(2027, 1)).thenReturn(List.of());
        when(clubDepartmentService.getOrCreate("회장단")).thenReturn(ClubDepartment.create("회장단"));
        when(forifTeamRepository.save(any(ForifTeam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delegate(USER_ID, targetId, "부회장");

        var member = org.mockito.ArgumentCaptor.forClass(ForifTeam.class);
        verify(forifTeamRepository).save(member.capture());
        assertEquals(targetId, member.getValue().getUser().getId());
        assertEquals(2027, member.getValue().getActYear());
        assertEquals(1, member.getValue().getActSemester());
        assertEquals("부회장", member.getValue().getUserTitle());
        assertEquals("회장단", member.getValue().getClubDepartment());
        assertEquals("회장", incumbent.getAffiliation());
        assertEquals("부회장", currentVice.getAffiliation());
        assertEquals("운영진", target.getAffiliation());
        verify(staffAccountRepository, never()).findByAffiliation("부회장");
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2026, 2, targetId))
                .thenReturn(Optional.empty());
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2027, 1, targetId))
                .thenReturn(Optional.of(member.getValue()));
        when(forifTeamRepository.findByYearAndSemester(2027, 1))
                .thenReturn(List.of(member.getValue()));
        assertEquals("부회장", service.getAuthorityAffiliation(targetId));
    }

    @Test
    void nextSemesterPresidentCannotResetAdminPassword() {
        account("운영진");
        upcoming("회장");
        Long incumbentId = 20260002L;
        StaffAccount incumbent = StaffAccount.createStaffAccount(
                User.createUser(incumbentId, "현 회장", "incumbent@example.com", "010-0000-0000", "컴퓨터학부"),
                "encoded", "현 회장", StaffRole.ADMIN, "회장");
        when(staffAccountRepository.findByUserIdAndRole(incumbentId, StaffRole.ADMIN))
                .thenReturn(Optional.of(incumbent));

        ForifException denied = assertThrows(ForifException.class,
                () -> service.updateAdmin(USER_ID, incumbentId, null, "ResetPass1!", null));

        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, denied.getErrorCode());
    }

    @Test
    void adminListReportsUpcomingAuthoritySeparatelyFromCurrentAffiliation() {
        User user = user();
        StaffAccount account = StaffAccount.createStaffAccount(
                user, "encoded", "운영진", StaffRole.ADMIN, "기획팀");
        ForifTeam team = ForifTeam.create(user, 2027, 1, "기획팀");
        team.update("회장", null, null, null, null);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByYearAndSemester(2026, 2)).thenReturn(List.of());
        when(forifTeamRepository.findByYearAndSemester(2027, 1)).thenReturn(List.of(team));

        Map<Long, StaffAccountService.AdminAuthority> authorities = service.getAdminAuthorities(List.of(account));

        assertEquals("기획팀", account.getAffiliation());
        assertEquals("회장", authorities.get(USER_ID).affiliation());
        assertFalse(authorities.get(USER_ID).swTeam());
        assertEquals("회장", authorities.get(USER_ID).upcomingAffiliation());
    }

    @Test
    void currentSwTeamAdminGetsPresidentAuthorityAndLosesItWhenMembershipEnds() {
        account("운영진");
        ForifTeam team = ForifTeam.create(user(), 2026, 2, "SW");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2026, 2, USER_ID))
                .thenReturn(Optional.of(team), Optional.of(team), Optional.empty());
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2027, 1, USER_ID))
                .thenReturn(Optional.empty());

        assertEquals("회장", service.getAuthorityAffiliation(USER_ID));
        assertDoesNotThrow(() -> service.requirePresident(USER_ID));
        assertEquals("운영진", service.getAuthorityAffiliation(USER_ID));
    }

    @Test
    void currentSwTeamAdminAppearsAsSwTeamAuthorityInAdminList() {
        StaffAccount account = StaffAccount.createStaffAccount(user(), "encoded", "운영진", StaffRole.ADMIN, "운영진");
        ForifTeam team = ForifTeam.create(user(), 2026, 2, "SW");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByYearAndSemester(2026, 2)).thenReturn(List.of(team));
        when(forifTeamRepository.findByYearAndSemester(2027, 1)).thenReturn(List.of());

        var authority = service.getAdminAuthorities(List.of(account)).get(USER_ID);

        assertEquals("회장", authority.affiliation());
        assertTrue(authority.swTeam());
        assertNull(authority.upcomingAffiliation());
    }

    @Test
    void upcomingPresidentDesignationRemainsVisibleForCurrentSwTeamMember() {
        StaffAccount account = StaffAccount.createStaffAccount(user(), "encoded", "운영진", StaffRole.ADMIN, "운영진");
        ForifTeam current = ForifTeam.create(user(), 2026, 2, "SW");
        ForifTeam next = ForifTeam.create(user(), 2027, 1, "회장단");
        next.update("회장", null, null, null, null);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByYearAndSemester(2026, 2)).thenReturn(List.of(current));
        when(forifTeamRepository.findByYearAndSemester(2027, 1)).thenReturn(List.of(next));

        var authority = service.getAdminAuthorities(List.of(account)).get(USER_ID);

        assertEquals("회장", authority.affiliation());
        assertTrue(authority.swTeam());
        assertEquals("회장", authority.upcomingAffiliation());
    }

    @Test
    void swTeamAdminCannotDelegateCurrentPresident() {
        account("운영진");
        ForifTeam team = ForifTeam.create(user(), 2026, 2, "SW");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2026, 2, USER_ID))
                .thenReturn(Optional.of(team));

        assertDoesNotThrow(() -> service.requirePresident(USER_ID));

        ForifException denied = assertThrows(ForifException.class,
                () -> service.delegate(USER_ID, 20260002L, "회장"));
        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, denied.getErrorCode());
    }

    @Test
    void presidentTeamCanCreateSwTeamAdminAccount() {
        account("운영진");
        upcoming("회장");
        Long targetId = 20260002L;
        User target = User.createUser(targetId, "새 운영진", "new@example.com", "010-0000-0000", "컴퓨터학부");
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode("ValidPass1!")).thenReturn("encoded");
        when(staffAccountRepository.save(any(StaffAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clubDepartmentService.getOrCreate("SW")).thenReturn(ClubDepartment.create("SW"));

        StaffAccount created = service.createAdmin(USER_ID, new CreateAdminCommand(targetId, "ValidPass1!", "SW"));

        assertEquals(targetId, created.getUserId());
        verify(forifTeamRepository).save(any(ForifTeam.class));
    }

    @Test
    void presidentTeamCanCreateAdminForExistingSwTeamMember() {
        account("운영진");
        upcoming("회장");
        Long targetId = 20260002L;
        User target = User.createUser(targetId, "SW 운영진", "sw@example.com", "010-0000-0000", "컴퓨터학부");
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode("ValidPass1!")).thenReturn("encoded");
        when(staffAccountRepository.save(any(StaffAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(forifTeamRepository.existsByActYearAndActSemesterAndUserId(2026, 2, targetId)).thenReturn(true);

        StaffAccount created = service.createAdmin(USER_ID, new CreateAdminCommand(targetId, "ValidPass1!", "기획"));

        assertEquals(targetId, created.getUserId());
        verify(forifTeamRepository, never()).save(any());
    }

    @Test
    void unappointedVicePresidentLosesRoleAtSemesterTransition() {
        StaffAccount incumbent = StaffAccount.createStaffAccount(
                User.createUser(20260002L, "현 부회장", "vice@example.com", "010-0000-0000", "컴퓨터학부"),
                "encoded", "현 부회장", StaffRole.ADMIN, "부회장");
        when(staffAccountRepository.findByAffiliation("부회장")).thenReturn(List.of(incumbent));

        service.handOverVicePresidency(null);

        assertEquals("운영진", incumbent.getAffiliation());
    }

    @Test
    void nextSemesterVicePresidentHasTeamAuthorityButNotPresidentAuthority() {
        account("운영진");
        upcoming("부회장");

        assertTrue(service.isPresidentTeam(USER_ID));
        ForifException denied = assertThrows(ForifException.class, () -> service.requirePresident(USER_ID));
        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, denied.getErrorCode());
    }

    @Test
    void titleOutsideNextSemesterDoesNotGrantAuthority() {
        account("운영진");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2026, 2, USER_ID))
                .thenReturn(Optional.empty());
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2027, 1, USER_ID))
                .thenReturn(Optional.empty());

        assertFalse(service.isPresidentTeam(USER_ID));
    }

    @Test
    void actualPresidentKeepsAuthorityWithoutNextSemesterRecord() {
        account("회장");

        assertDoesNotThrow(() -> service.requirePresident(USER_ID));
    }

    @Test
    void nextSemesterPresidentCanTakeOverFromActualPresident() {
        account("운영진");
        upcoming("회장");
        StaffAccount incumbent = StaffAccount.createStaffAccount(
                User.createUser(20260002L, "현 회장", "incumbent@example.com", "010-0000-0000", "컴퓨터학부"),
                "encoded", "현 회장", StaffRole.ADMIN, "회장");
        when(staffAccountRepository.findByAffiliation("회장")).thenReturn(List.of(incumbent));

        service.handOverPresidency(USER_ID, USER_ID);

        assertEquals("회장", service.getStaffInfo(USER_ID).getAffiliation());
        assertEquals("운영진", incumbent.getAffiliation());
    }

    @Test
    void removingNextSemesterTitleImmediatelyRevokesAuthority() {
        account("운영진");
        ForifTeam team = ForifTeam.create(user(), 2027, 1, "회장단");
        team.update("회장", null, null, null, null);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2026, 2, USER_ID))
                .thenReturn(Optional.empty());
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2027, 1, USER_ID))
                .thenReturn(Optional.of(team), Optional.empty());
        when(forifTeamRepository.findByYearAndSemester(2027, 1)).thenReturn(List.of(team));

        assertTrue(service.isPresidentTeam(USER_ID));
        assertFalse(service.isPresidentTeam(USER_ID));
    }

    private void account(String affiliation) {
        User user = user();
        StaffAccount account = StaffAccount.createStaffAccount(user, "encoded", "운영진", StaffRole.ADMIN, affiliation);
        when(staffAccountRepository.findByUserIdAndRole(USER_ID, StaffRole.ADMIN)).thenReturn(Optional.of(account));
    }

    private void upcoming(String title) {
        ForifTeam team = ForifTeam.create(user(), 2027, 1, "기획팀");
        team.update(title, null, null, null, null);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2026, 2, USER_ID))
                .thenReturn(Optional.empty());
        when(forifTeamRepository.findByActYearAndActSemesterAndUserId(2027, 1, USER_ID))
                .thenReturn(Optional.of(team));
        when(forifTeamRepository.findByYearAndSemester(2027, 1)).thenReturn(List.of(team));
    }

    private User user() {
        return User.createUser(USER_ID, "운영진", "staff@example.com", "010-0000-0000", "컴퓨터학부");
    }
}
