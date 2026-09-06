package org.forif_backend.application.user;

import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceMemberInfoTest {

    private static final Long USER_ID = 20260001L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserApplyRepository userApplyRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StaffAccountRepository staffAccountRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void updatesOnlyDepartmentAndPhoneNumber() {
        User member = User.createUser(
                USER_ID,
                "부원",
                "member@hanyang.ac.kr",
                "010-1111-2222",
                "컴퓨터학부"
        );
        member.updateProfile(null, "users/profiles/member.png");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(member));

        userService.updateMemberInfo(USER_ID, "소프트웨어학부", "010-3333-4444");

        assertThat(member.getId()).isEqualTo(USER_ID);
        assertThat(member.getUserName()).isEqualTo("부원");
        assertThat(member.getDepartment()).isEqualTo("소프트웨어학부");
        assertThat(member.getPhoneNum()).isEqualTo("010-3333-4444");
        assertThat(member.getImgUrl()).isEqualTo("users/profiles/member.png");
    }

    @Test
    void showsAcceptedStudyBeforeStudyUserIsCreated() {
        User acceptedUser = User.createUser(
                USER_ID,
                "합격자",
                "accepted@hanyang.ac.kr",
                "010-1111-2222",
                "컴퓨터학부"
        );
        when(userRepository.countRegularStudyAcceptedApplicantsByYearSemester(2026, 1, null)).thenReturn(1L);
        when(userRepository.searchRegularStudyAcceptedApplicantsByYearSemester(2026, 1, null, 100, null))
                .thenReturn(List.of(acceptedUser));
        when(studyRepository.findCurrentStudyNamesByUserIds(List.of(USER_ID), 2026, 1)).thenReturn(Map.of());
        when(userApplyRepository.findAcceptedStudyNamesByUserIdsAndYearSemester(List.of(USER_ID), 2026, 1))
                .thenReturn(Map.of(USER_ID, "확정 스터디"));
        when(studyRepository.findMentorUserIdsByUserIds(List.of(USER_ID), 2026, 1)).thenReturn(Set.of());
        when(staffAccountRepository.findStaffRolesByUserIds(List.of(USER_ID))).thenReturn(Map.of());

        var result = userService.getRegularStudyAcceptedApplicants(2026, 1, null, 100, null);

        assertThat(result.content()).singleElement()
                .extracting(info -> info.currentStudyName())
                .isEqualTo("확정 스터디");
    }
}
