package org.forif_backend.application.user;

import jakarta.validation.Validator;
import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.study.StudyMentorAccess;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.application.user.dto.AdminStudyApplicationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplyServiceAdminApplicationsTest {

    @Mock private SemesterService semesterService;
    @Mock private SemesterPhaseGuard semesterPhaseGuard;
    @Mock private StudyMentorAccess studyMentorAccess;
    @Mock private DuesService duesService;
    @Mock private UserRepository userRepository;
    @Mock private UserApplyRepository userApplyRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private StudyUserRepository studyUserRepository;
    @Mock private Validator validator;

    @InjectMocks private UserApplyService userApplyService;

    @Test
    void returnsFinalStoredApplicationDetailsAndSortsBeforePaging() {
        User firstApplicant = User.createUser(20260001L, "첫 신청자", "first@forif.org", "01011112222", "컴퓨터학부");
        User secondApplicant = User.createUser(20260002L, "둘 신청자", "second@forif.org", "01033334444", "전자공학부");
        UserApply first = application(firstApplicant, "최종 Z 스터디", "최종 A 스터디", 11);
        UserApply second = application(secondApplicant, "최종 M 스터디", null, null);
        when(userApplyRepository.findAllByYearSemester(2026, 2)).thenReturn(List.of(first, second));

        CursorPageResponse<AdminStudyApplicationInfo> firstPage = userApplyService.getAdminApplications(
                2026, 2, 0, 2, null, List.of(new SortCriteria("studyName", SortDirection.ASC)));
        CursorPageResponse<AdminStudyApplicationInfo> secondPage = userApplyService.getAdminApplications(
                2026, 2, 1, 2, null, List.of(new SortCriteria("studyName", SortDirection.ASC)));

        assertThat(firstPage.content()).extracting(AdminStudyApplicationInfo::studyName)
                .containsExactly("최종 A 스터디", "최종 M 스터디");
        assertThat(secondPage.content()).extracting(AdminStudyApplicationInfo::studyName)
                .containsExactly("최종 Z 스터디");
        assertThat(firstPage.totalElements()).isEqualTo(3);
        verifyNoInteractions(studyRepository);
    }

    private UserApply application(
            User applicant,
            String primaryStudyName,
            String secondaryStudyName,
            Integer secondaryStudy
    ) {
        UserApply apply = org.mockito.Mockito.mock(UserApply.class);
        when(apply.getApplier()).thenReturn(applicant);
        when(apply.getPrimaryStudyName()).thenReturn(primaryStudyName);
        when(apply.getSecondaryStudy()).thenReturn(secondaryStudy);
        if (secondaryStudy != null) {
            when(apply.getSecondaryStudyName()).thenReturn(secondaryStudyName);
        }
        when(apply.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 8, 1, 12, 0));
        return apply;
    }
}
