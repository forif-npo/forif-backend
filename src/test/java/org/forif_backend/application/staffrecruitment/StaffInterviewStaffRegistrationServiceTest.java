package org.forif_backend.application.staffrecruitment;

import org.forif_backend.application.staffrecruitment.dto.StaffRegistrationResult;
import org.forif_backend.application.team.ForifTeamService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.*;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffInterviewStaffRegistrationServiceTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Integer RECRUITMENT_ID = 12;

    @Mock private StaffRecruitmentRepository staffRecruitmentRepository;
    @Mock private StaffInterviewRecordRepository staffInterviewRecordRepository;
    @Mock private ForifTeamRepository forifTeamRepository;
    @Mock private ForifTeamService forifTeamService;

    @InjectMocks
    private StaffInterviewStaffRegistrationService service;

    private StaffRecruitment recruitment;
    private StaffRecruitmentDepartment sw;
    private StaffRecruitmentDepartment planning;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        recruitment = StaffRecruitment.create(2027, 1, "2027-1 운영진 모집", null, null,
                now.minusDays(1), now.plusDays(1));
        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "SW", null, 1, true),
                new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 2, true)), false);
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);
        sw = recruitment.getDepartments().get(0);
        planning = recruitment.getDepartments().get(1);
        ReflectionTestUtils.setField(sw, "id", 31);
        ReflectionTestUtils.setField(planning, "id", 32);
        // 지원서는 접수 중에만 낼 수 있어서, 각 테스트가 기록을 만든 뒤 givenRecords로 마감한다.
        recruitment.publish();

        when(staffRecruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
    }

    private void givenRecords(StaffInterviewRecord... records) {
        recruitment.close("면접", LocalDateTime.now());
        recruitment.finalizeInterviewResults(LocalDateTime.now());
        when(staffInterviewRecordRepository.findRecordsByRecruitmentId(RECRUITMENT_ID)).thenReturn(List.of(records));
    }

    private StaffInterviewRecord record(User applicant, String name, StaffInterviewStatus status,
                                        StaffRecruitmentDepartment passedDepartment) {
        StaffApplication application = StaffApplication.submit(
                recruitment, applicant, name, name + "@example.com", null, null, LocalDateTime.now());
        return StaffInterviewRecord.create(application, status, passedDepartment);
    }

    private User user(long id, String name) {
        return User.createUser(id, name, id + "@hanyang.ac.kr", "01000000000", "컴퓨터학부");
    }

    @Test
    void 합격자를_모집_회차의_활동_학기에_배정_부서로_올린다() {
        givenRecords(
                record(user(10L, "가합격"), "가합격", StaffInterviewStatus.PASSED, planning),
                record(user(11L, "나불합격"), "나불합격", StaffInterviewStatus.FAILED, null),
                record(user(12L, "다완료"), "다완료", StaffInterviewStatus.DONE, null));

        StaffRegistrationResult result = service.registerPassedApplicants(REQUESTER_ID, RECRUITMENT_ID);

        verify(forifTeamService).addFinalizedRecruitmentMember(REQUESTER_ID, 10L, 2027, 1, "기획팀");
        verify(forifTeamService, never()).addFinalizedRecruitmentMember(any(), eq(11L), anyInt(), anyInt(), any());
        verify(forifTeamService, never()).addFinalizedRecruitmentMember(any(), eq(12L), anyInt(), anyInt(), any());
        assertThat(result.actYear()).isEqualTo(2027);
        assertThat(result.actSemester()).isEqualTo(1);
        assertThat(result.registered()).containsExactly("가합격");
    }

    @Test
    void 이미_명단에_있는_합격자는_건너뛰어_여러_번_눌러도_된다() {
        givenRecords(
                record(user(10L, "가합격"), "가합격", StaffInterviewStatus.PASSED, sw));
        when(forifTeamRepository.existsByActYearAndActSemesterAndUserId(2027, 1, 10L)).thenReturn(true);

        StaffRegistrationResult result = service.registerPassedApplicants(REQUESTER_ID, RECRUITMENT_ID);

        verify(forifTeamService, never()).addFinalizedRecruitmentMember(any(), anyLong(), anyInt(), anyInt(), any());
        assertThat(result.registered()).isEmpty();
        assertThat(result.alreadyMembers()).containsExactly("가합격");
    }

    @Test
    void 탈퇴한_합격자는_올리지_않고_따로_알려_준다() {
        givenRecords(
                record(null, "탈퇴자", StaffInterviewStatus.PASSED, sw));

        StaffRegistrationResult result = service.registerPassedApplicants(REQUESTER_ID, RECRUITMENT_ID);

        verify(forifTeamService, never()).addFinalizedRecruitmentMember(any(), anyLong(), anyInt(), anyInt(), any());
        assertThat(result.withdrawn()).containsExactly("탈퇴자");
    }

    @Test
    void 확정된_차기_학기_SW_합격자를_회장단이_등록한다() {
        givenRecords(
                record(user(10L, "가합격"), "가합격", StaffInterviewStatus.PASSED, sw));

        StaffRegistrationResult result = service.registerPassedApplicants(REQUESTER_ID, RECRUITMENT_ID);

        verify(forifTeamService).addFinalizedRecruitmentMember(REQUESTER_ID, 10L, 2027, 1, "SW");
        assertThat(result.registered()).containsExactly("가합격");
    }

    @Test
    void 마감_전에는_등록할_수_없다() {
        LocalDateTime now = LocalDateTime.now();
        StaffRecruitment open = StaffRecruitment.create(2027, 1, "모집", null, null, now.minusDays(1), now.plusDays(1));
        open.publish();
        when(staffRecruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(Optional.of(open));

        assertThatThrownBy(() -> service.registerPassedApplicants(REQUESTER_ID, RECRUITMENT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
    }

    @Test
    void 면접_결과_확정_전에는_명단에_등록할_수_없다() {
        recruitment.close("면접", LocalDateTime.now());

        assertThatThrownBy(() -> service.registerPassedApplicants(REQUESTER_ID, RECRUITMENT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_RESULTS_NOT_FINALIZED);
        verify(forifTeamService, never()).addFinalizedRecruitmentMember(any(), anyLong(), anyInt(), anyInt(), any());
    }
}
