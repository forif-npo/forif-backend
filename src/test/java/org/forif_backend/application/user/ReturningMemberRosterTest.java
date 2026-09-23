package org.forif_backend.application.user;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.user.dto.ReturningMemberRoster;
import org.forif_backend.domain.department.College;
import org.forif_backend.domain.department.Department;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturningMemberRosterTest {
    @Mock SemesterService semesterService;
    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @ParameterizedTest
    @CsvSource({"2026,1,2025,2", "2026,2,2026,1"})
    void calculatesPreviousSemesterFromTheConfiguredSemester(int year, int semester, int previousYear, int previousSemester) {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(year, semester));
        User legacy = User.createUser(910001L, "가상부원", "member@example.invalid", null, "가상학과");
        when(userRepository.findReturningMembers(year, semester, previousYear, previousSemester)).thenReturn(List.of(legacy));

        ReturningMemberRoster roster = userService.getReturningMemberRoster();

        assertThat(roster.actYear()).isEqualTo(year);
        assertThat(roster.actSemester()).isEqualTo(semester);
        assertThat(roster.members()).containsExactly(new ReturningMemberRoster.Member(
                "910001", "가상부원", "", "가상학과", null));
        verify(semesterService, times(1)).getActive();
    }

    @Test
    void includesCollegeFromTheLinkedDepartment() {
        User user = User.createUser(910001L, "가상부원", "member@example.invalid", "01000000000", "이전 학과");
        Department department = mock(Department.class);
        College college = mock(College.class);
        when(department.getDepartmentName()).thenReturn("가상학과");
        when(department.getCollege()).thenReturn(college);
        when(college.getCollegeName()).thenReturn("가상대학");
        user.updateDepartment(department);

        assertThat(ReturningMemberRoster.Member.from(user)).isEqualTo(new ReturningMemberRoster.Member(
                "910001", "가상부원", "가상대학", "가상학과", "01000000000"));
    }
}
