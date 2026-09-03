package org.forif_backend.application.team;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.user.UserService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.team.ForifTeam;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @InjectMocks private ForifTeamService forifTeamService;

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
        when(forifTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(staffAccountService.isPresidentTeam(OTHER_OPERATOR_ID)).thenReturn(true);

        forifTeamService.updateMember(
                OTHER_OPERATOR_ID, TEAM_ID, "부회장", "기획팀", "소개", "자기소개", 2028);

        assertEquals("부회장", team.getUserTitle());
        assertEquals("기획팀", team.getClubDepartment());
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

    private ForifTeam team(Long userId) {
        User user = User.createUser(userId, "운영진", userId + "@hanyang.ac.kr", "010-0000-0000", "컴퓨터학부");
        ForifTeam team = ForifTeam.create(user, 2026, 1, "개발팀");
        team.update("운영진", null, null, null, null);
        return team;
    }
}
