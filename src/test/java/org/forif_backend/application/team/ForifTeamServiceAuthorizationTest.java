package org.forif_backend.application.team;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.user.UserService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ClubDepartment;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForifTeamServiceAuthorizationTest {

    private static final Long OWNER_ID = 20260001L;
    private static final Long OTHER_OPERATOR_ID = 20260002L;
    private static final Long TEAM_ID = 1L;

    @Mock private ForifTeamRepository forifTeamRepository;
    @Mock private UserRepository userRepository;
    @Mock private SemesterService semesterService;
    @Mock private UserService userService;
    @Mock private StaffAccountService staffAccountService;
    @Mock private ClubDepartmentService clubDepartmentService;
    @InjectMocks private ForifTeamService forifTeamService;

    @Test
    void presidentTeamCanRegisterFinalizedRecruitmentSw() {
        User applicant = User.createUser(OWNER_ID, "합격자", "applicant@example.com", "01000000000", "컴퓨터학부");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(userRepository.findUserById(OWNER_ID)).thenReturn(Optional.of(applicant));
        when(clubDepartmentService.getOrCreate("SW")).thenReturn(ClubDepartment.create("SW"));
        when(forifTeamRepository.save(any(ForifTeam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = forifTeamService.addFinalizedRecruitmentMember(OTHER_OPERATOR_ID, OWNER_ID, 2027, 1, "SW");

        assertEquals("SW", result.clubDepartment());
        assertEquals("팀원", result.userTitle());
        verify(staffAccountService).requirePresidentTeam(OTHER_OPERATOR_ID);
        verify(staffAccountService, never()).requireIncumbentPresident(OTHER_OPERATOR_ID);
    }

    @Test
    void presidentTeamCanAddNextSemesterSw() {
        User applicant = User.createUser(OWNER_ID, "합격자", "applicant@example.com", "01000000000", "컴퓨터학부");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(userRepository.findUserById(OWNER_ID)).thenReturn(Optional.of(applicant));
        when(clubDepartmentService.getOrCreate("SW팀")).thenReturn(ClubDepartment.create("SW"));
        when(forifTeamRepository.save(any(ForifTeam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = forifTeamService.addMember(OTHER_OPERATOR_ID, OWNER_ID, 2027, 1, "SW팀", "팀원");

        assertEquals("SW", result.clubDepartment());
        verify(staffAccountService, never()).requireIncumbentPresident(OTHER_OPERATOR_ID);
    }

    @Test
    void presidentTeamCanAddCurrentSemesterSw() {
        User applicant = User.createUser(OWNER_ID, "운영진", "applicant@example.com", "01000000000", "컴퓨터학부");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(userRepository.findUserById(OWNER_ID)).thenReturn(Optional.of(applicant));
        when(clubDepartmentService.getOrCreate("SW")).thenReturn(ClubDepartment.create("SW"));
        when(forifTeamRepository.save(any(ForifTeam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = forifTeamService.addMember(OTHER_OPERATOR_ID, OWNER_ID, null, null, "SW", "팀원");

        assertEquals("SW", result.clubDepartment());
        assertEquals(2026, result.actYear());
        assertEquals(2, result.actSemester());
        verify(staffAccountService, never()).requireIncumbentPresident(OTHER_OPERATOR_ID);
    }

    @Test
    void presidentTeamCanRegisterCurrentSemesterSw() {
        User applicant = User.createUser(OWNER_ID, "합격자", "applicant@example.com", "01000000000", "컴퓨터학부");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(userRepository.findUserById(OWNER_ID)).thenReturn(Optional.of(applicant));
        when(clubDepartmentService.getOrCreate("SW")).thenReturn(ClubDepartment.create("SW"));
        when(forifTeamRepository.save(any(ForifTeam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = forifTeamService.addFinalizedRecruitmentMember(OTHER_OPERATOR_ID, OWNER_ID, 2026, 2, "SW");

        assertEquals("SW", result.clubDepartment());
        verify(staffAccountService).requirePresidentTeam(OTHER_OPERATOR_ID);
        verify(staffAccountService, never()).requireIncumbentPresident(OTHER_OPERATOR_ID);
    }

    @Test
    void generalOperatorCanUpdateOnlyOwnPublicProfileFields() {
        ForifTeam team = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OWNER_ID)).thenReturn(false);

        forifTeamService.updateMember(
                OWNER_ID, TEAM_ID, null, null,
                "백엔드, 커피러버", "안녕하세요", 2027);

        assertEquals("운영진", team.getUserTitle());
        assertEquals("개발팀", team.getClubDepartment());
        assertEquals("백엔드, 커피러버", team.getIntroTag());
        assertEquals("안녕하세요", team.getSelfIntro());
        assertEquals(2027, team.getGraduateYear());
    }

    @Test
    void generalOperatorCanSubmitUnchangedTitleAndDepartmentWithProfileUpdates() {
        ForifTeam team = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OWNER_ID)).thenReturn(false);

        forifTeamService.updateMember(
                OWNER_ID, TEAM_ID, "운영진", "개발팀",
                "백엔드, 커피러버", "안녕하세요", 2027);

        assertEquals("운영진", team.getUserTitle());
        assertEquals("개발팀", team.getClubDepartment());
        assertEquals("백엔드, 커피러버", team.getIntroTag());
        assertEquals("안녕하세요", team.getSelfIntro());
        assertEquals(2027, team.getGraduateYear());
    }

    @Test
    void generalOperatorCannotChangeOwnTitleOrDepartment() {
        ForifTeam team = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OWNER_ID)).thenReturn(false);

        ForifException exception = assertThrows(
                ForifException.class,
                () -> forifTeamService.updateMember(
                        OWNER_ID, TEAM_ID, "회장", null, "소개", "자기소개", 2027));

        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.getErrorCode());
        assertEquals("운영진", team.getUserTitle());
        assertEquals("개발팀", team.getClubDepartment());
    }

    @Test
    void generalOperatorCannotUpdateAnotherOperatorsProfile() {
        ForifTeam team = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(false);

        ForifException exception = assertThrows(
                ForifException.class,
                () -> forifTeamService.updateMember(
                        OTHER_OPERATOR_ID, TEAM_ID, null, null, "소개", "자기소개", 2027));

        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.getErrorCode());
    }

    @Test
    void nonAdminRequesterCannotUpdateAnOperatorsProfile() {
        ForifTeam team = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID))
                .thenThrow(new ForifException(ErrorCode.STAFF_NOT_FOUND));

        ForifException exception = assertThrows(
                ForifException.class,
                () -> forifTeamService.updateMember(
                        OTHER_OPERATOR_ID, TEAM_ID, null, null, "소개", "자기소개", 2027));

        assertEquals(ErrorCode.STAFF_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void presidentTeamCanUpdateAnyProfileField() {
        ForifTeam team = team(OWNER_ID);
        ClubDepartment planning = ClubDepartment.create("기획팀");
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(true);
        when(clubDepartmentService.getOrCreate("기획팀")).thenReturn(planning);

        forifTeamService.updateMember(
                OTHER_OPERATOR_ID, TEAM_ID, "팀장", "기획팀", "소개", "자기소개", 2028);

        assertEquals("팀장", team.getUserTitle());
        assertEquals("기획팀", team.getClubDepartment());
        assertEquals(planning, team.getClubDepartmentRef());
        assertEquals("소개", team.getIntroTag());
        assertEquals("자기소개", team.getSelfIntro());
        assertEquals(2028, team.getGraduateYear());
    }

    @Test
    void generalOperatorCanUpdateOwnProfileImage() {
        ForifTeam team = team(OWNER_ID);
        MockMultipartFile image = new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1});
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OWNER_ID)).thenReturn(false);

        forifTeamService.updateMemberProfileImage(OWNER_ID, TEAM_ID, image);

        verify(userService).updateUserProfileImage(OWNER_ID, image);
    }

    @Test
    void generalOperatorCannotUpdateAnotherOperatorsProfileImage() {
        ForifTeam team = team(OWNER_ID);
        MockMultipartFile image = new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1});
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(false);

        ForifException exception = assertThrows(
                ForifException.class,
                () -> forifTeamService.updateMemberProfileImage(OTHER_OPERATOR_ID, TEAM_ID, image));

        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.getErrorCode());
        verify(userService, never()).updateUserProfileImage(OWNER_ID, image);
    }

    @Test
    void vicePresidentCannotAppointNextSemesterPresident() {
        ForifTeam member = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(member));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(true);
        org.mockito.Mockito.doThrow(new ForifException(ErrorCode.INSUFFICIENT_PERMISSION))
                .when(staffAccountService).requireIncumbentPresident(OTHER_OPERATOR_ID);

        ForifException denied = assertThrows(ForifException.class,
                () -> forifTeamService.updateMember(OTHER_OPERATOR_ID, TEAM_ID, "회장", null, null, null, null));

        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, denied.getErrorCode());
        assertEquals("운영진", member.getUserTitle());
    }

    @Test
    void presidentTeamCanAssignSwTeamMembership() {
        ForifTeam member = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(member));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(true);
        when(clubDepartmentService.getOrCreate("SW")).thenReturn(ClubDepartment.create("SW"));

        forifTeamService.updateMember(OTHER_OPERATOR_ID, TEAM_ID,
                null, "SW", null, null, null);

        assertEquals("SW", member.getClubDepartment());
        verify(staffAccountService, never()).requireIncumbentPresident(OTHER_OPERATOR_ID);
    }

    @Test
    void presidentTeamCanDeleteSwTeamMembership() {
        ForifTeam member = ForifTeam.create(
                User.createUser(OWNER_ID, "운영진", "owner@example.com", "010-0000-0000", "컴퓨터학부"),
                2026, 1, "SW");
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(member));
        forifTeamService.deleteMember(OTHER_OPERATOR_ID, TEAM_ID);

        verify(forifTeamRepository).deleteById(TEAM_ID);
        verify(staffAccountService, never()).requireIncumbentPresident(OTHER_OPERATOR_ID);
    }

    @Test
    void incumbentCanAppointNextSemesterPresidentWithAdminAccount() {
        ForifTeam member = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(member));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(true);
        when(staffAccountService.isAdminAccount(OWNER_ID)).thenReturn(true);
        when(forifTeamRepository.findByYearAndSemester(2026, 1)).thenReturn(List.of(member));

        forifTeamService.updateMember(OTHER_OPERATOR_ID, TEAM_ID, "회장", null, null, null, null);

        assertEquals("회장", member.getUserTitle());
        verify(staffAccountService).requireIncumbentPresident(OTHER_OPERATOR_ID);
    }

    @Test
    void leadershipTitleCannotBeAssignedWithoutAdminAccount() {
        ForifTeam member = team(OWNER_ID);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(member));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(true);

        ForifException denied = assertThrows(ForifException.class,
                () -> forifTeamService.updateMember(OTHER_OPERATOR_ID, TEAM_ID, "회장", null, null, null, null));

        assertEquals(ErrorCode.STAFF_NOT_FOUND, denied.getErrorCode());
        assertEquals("운영진", member.getUserTitle());
    }

    @Test
    void secondPresidentCannotBeAssignedInOneSemester() {
        ForifTeam member = team(OWNER_ID);
        ForifTeam assigned = team(20260003L);
        assigned.update("회장", null, null, null, null);
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(member));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(true);
        when(staffAccountService.isAdminAccount(OWNER_ID)).thenReturn(true);
        when(forifTeamRepository.findByYearAndSemester(2026, 1)).thenReturn(List.of(member, assigned));

        ForifException denied = assertThrows(ForifException.class,
                () -> forifTeamService.updateMember(OTHER_OPERATOR_ID, TEAM_ID, "회장", null, null, null, null));

        assertEquals(ErrorCode.FORIF_TEAM_LEADERSHIP_ALREADY_EXISTS, denied.getErrorCode());
        assertEquals("운영진", member.getUserTitle());
    }

    private ForifTeam team(Long userId) {
        User user = User.createUser(userId, "운영진", userId + "@hanyang.ac.kr", "010-0000-0000", "컴퓨터학부");
        ForifTeam team = ForifTeam.create(user, 2026, 1, "개발팀");
        team.update("운영진", null, null, null, null);
        return team;
    }
}
