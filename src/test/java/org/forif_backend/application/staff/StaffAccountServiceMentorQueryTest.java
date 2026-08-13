package org.forif_backend.application.staff;

import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.staff.dto.MentorSummary;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.staff.StaffAccountRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceMentorQueryTest {

    @Mock private SemesterService semesterService;
    @Mock private StaffAccountRepository staffAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private ForifTeamRepository forifTeamRepository;
    @Mock private StudyRepository studyRepository;
    @InjectMocks private StaffAccountService staffAccountService;

    @Test
    void marksOnlyLegacyMentorAccountsAsManageable() {
        User manageableMentor = User.createUser(1L, "기존 멘토", "legacy@forif.org", "010", "컴퓨터공학과");
        User studyOnlyMentor = User.createUser(2L, "신규 멘토", "study@forif.org", "010", "컴퓨터공학과");
        when(studyRepository.countMentors(null)).thenReturn(2L);
        when(studyRepository.searchMentors(null, 20, null)).thenReturn(List.of(manageableMentor, studyOnlyMentor));
        when(studyRepository.findMentorStudyNamesByUserIds(List.of(1L, 2L), null, null))
                .thenReturn(Map.of(1L, "기존 스터디", 2L, "신규 스터디"));
        when(staffAccountRepository.findMentorAccountUserIdsByUserIds(List.of(1L, 2L)))
                .thenReturn(Set.of(1L));

        CursorPageResponse<MentorSummary> result = staffAccountService.getMentors(null, null, 20, null);

        assertThat(result.content()).extracting(MentorSummary::manageable)
                .containsExactly(true, false);
    }
}
