package org.forif_backend.application.staffrecruitment;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationDetail;
import org.forif_backend.application.staffrecruitment.dto.SubmitStaffApplicationCommand;
import org.forif_backend.application.staffrecruitment.dto.SubmitStaffApplicationCommand.AnswerCommand;
import org.forif_backend.application.staffrecruitment.dto.SubmitStaffApplicationCommand.ChoiceCommand;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentDepartment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestion;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffApplicationServiceTest {

    private static final Integer RECRUITMENT_ID = 12;
    private static final Integer PLANNING_TEAM = 31;
    private static final Integer DEV_TEAM = 32;
    private static final Integer CLOSED_TEAM = 33;
    private static final Integer MOTIVE_QUESTION = 41;
    private static final Long APPLICANT_ID = 2020123456L;

    @Mock
    private StaffApplicationRepository staffApplicationRepository;

    @Mock
    private StaffRecruitmentRepository staffRecruitmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyUserRepository studyUserRepository;

    @Mock
    private SemesterService semesterService;

    @InjectMocks
    private StaffApplicationService staffApplicationService;

    private final User applicant =
            User.createUser(APPLICANT_ID, "홍길동", "hong@hanyang.ac.kr", "01000000000", "컴퓨터소프트웨어학부");

    private StaffRecruitment openRecruitment() {
        LocalDateTime now = LocalDateTime.now();
        return recruitment(now.minusDays(1), now.plusDays(1));
    }

    private StaffRecruitment recruitment(LocalDateTime startsAt, LocalDateTime endsAt) {
        StaffRecruitment recruitment =
                StaffRecruitment.create(2027, 1, "2027-1 운영진 모집", null, null, startsAt, endsAt);
        recruitment.publish();
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);

        StaffRecruitmentDepartment planning = StaffRecruitmentDepartment.create("기획팀", null, 1, true);
        StaffRecruitmentDepartment dev = StaffRecruitmentDepartment.create("개발팀", null, 2, true);
        StaffRecruitmentDepartment closed = StaffRecruitmentDepartment.create("디자인팀", null, 3, false);
        ReflectionTestUtils.setField(planning, "id", PLANNING_TEAM);
        ReflectionTestUtils.setField(dev, "id", DEV_TEAM);
        ReflectionTestUtils.setField(closed, "id", CLOSED_TEAM);
        recruitment.replaceDepartments(List.of(planning, dev, closed));

        StaffRecruitmentQuestion motive = StaffRecruitmentQuestion.create(
                "지원 동기", null, StaffRecruitmentQuestionType.LONG_TEXT, true, 100, 1);
        ReflectionTestUtils.setField(motive, "id", MOTIVE_QUESTION);
        recruitment.replaceQuestions(List.of(motive));

        return recruitment;
    }

    private SubmitStaffApplicationCommand command(List<ChoiceCommand> choices, List<AnswerCommand> answers) {
        return new SubmitStaffApplicationCommand(choices, answers);
    }

    private List<ChoiceCommand> firstChoiceOnly() {
        return List.of(new ChoiceCommand(PLANNING_TEAM, 1));
    }

    private List<AnswerCommand> validAnswers() {
        return List.of(new AnswerCommand(MOTIVE_QUESTION, "함께 운영하고 싶습니다."));
    }

    private void givenOpenRecruitment() {
        when(staffRecruitmentRepository.findDetailById(RECRUITMENT_ID)).thenReturn(Optional.of(openRecruitment()));
    }

    private void givenActiveMember() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(studyUserRepository.existsByUserIdAndStudyYearSemester(APPLICANT_ID, 2026, 2)).thenReturn(true);
    }

    private void givenApplicantLoaded() {
        when(staffApplicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, APPLICANT_ID))
                .thenReturn(false);
        when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
    }

    @Test
    void 이번_학기_활동_부원이_아니면_제출할_수_없다() {
        givenOpenRecruitment();
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(studyUserRepository.existsByUserIdAndStudyYearSemester(APPLICANT_ID, 2026, 2)).thenReturn(false);

        assertThatThrownBy(() -> staffApplicationService.submit(
                RECRUITMENT_ID, APPLICANT_ID, command(firstChoiceOnly(), validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_ELIGIBILITY_REQUIRED);
        verify(staffApplicationRepository, never()).save(any());
    }

    @Test
    void 같은_회차에_두_번_제출할_수_없다() {
        givenOpenRecruitment();
        givenActiveMember();
        when(staffApplicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, APPLICANT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> staffApplicationService.submit(
                RECRUITMENT_ID, APPLICANT_ID, command(firstChoiceOnly(), validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_ALREADY_EXISTS);
        verify(staffApplicationRepository, never()).save(any());
    }

    @Test
    void 접수_기간이_아니면_제출할_수_없다() {
        LocalDateTime now = LocalDateTime.now();
        when(staffRecruitmentRepository.findDetailById(RECRUITMENT_ID))
                .thenReturn(Optional.of(recruitment(now.minusDays(10), now.minusDays(1))));
        givenActiveMember();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(
                RECRUITMENT_ID, APPLICANT_ID, command(firstChoiceOnly(), validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_NOT_OPEN);
        verify(staffApplicationRepository, never()).save(any());
    }

    @Test
    void 일지망이_없으면_제출할_수_없다() {
        givenOpenRecruitment();
        givenActiveMember();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(List.of(new ChoiceCommand(PLANNING_TEAM, 2)), validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
    }

    @Test
    void 같은_부서를_두_지망에_넣을_수_없다() {
        givenOpenRecruitment();
        givenActiveMember();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(List.of(new ChoiceCommand(PLANNING_TEAM, 1), new ChoiceCommand(PLANNING_TEAM, 2)),
                        validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
    }

    @Test
    void 모집하지_않는_부서는_고를_수_없다() {
        givenOpenRecruitment();
        givenActiveMember();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(List.of(new ChoiceCommand(CLOSED_TEAM, 1)), validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
    }

    @Test
    void 필수_문항에_답하지_않으면_제출할_수_없다() {
        givenOpenRecruitment();
        givenActiveMember();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
    }

    @Test
    void 문항의_최대_길이를_넘으면_제출할_수_없다() {
        givenOpenRecruitment();
        givenActiveMember();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, "가".repeat(101))))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
    }

    @Test
    void 다른_회차의_문항에는_답할_수_없다() {
        givenOpenRecruitment();
        givenActiveMember();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(9999, "다른 회차 문항")))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
    }

    @Test
    void 제출하면_그_시점의_개인정보와_지망_답변을_저장한다() {
        givenOpenRecruitment();
        givenActiveMember();
        givenApplicantLoaded();
        when(staffApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffApplicationDetail detail = staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(List.of(new ChoiceCommand(PLANNING_TEAM, 1), new ChoiceCommand(DEV_TEAM, 2)),
                        validAnswers()));

        assertThat(detail.applicantName()).isEqualTo("홍길동");
        assertThat(detail.applicantEmail()).isEqualTo("hong@hanyang.ac.kr");
        assertThat(detail.applicantDepartment()).isEqualTo("컴퓨터소프트웨어학부");
        assertThat(detail.appliedDepartments()).hasSize(2);
        assertThat(detail.appliedDepartments().get(0).priority()).isEqualTo(1);
        assertThat(detail.appliedDepartments().get(0).name()).isEqualTo("기획팀");
        assertThat(detail.answers()).hasSize(1);
        assertThat(detail.answers().get(0).questionLabel()).isEqualTo("지원 동기");
        verify(staffApplicationRepository).flush();
    }

    @Test
    void 제출하지_않았으면_본인_조회는_404다() {
        when(staffApplicationRepository.findDetailByRecruitmentIdAndApplicantId(RECRUITMENT_ID, APPLICANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffApplicationService.getMine(RECRUITMENT_ID, APPLICANT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_NOT_FOUND);
    }

    @Test
    void 다른_회차의_지원서는_관리_상세에서도_찾을_수_없다() {
        StaffRecruitment other = openRecruitment();
        ReflectionTestUtils.setField(other, "id", 99);
        StaffApplication application = StaffApplication.submit(other, applicant, "홍길동",
                "hong@hanyang.ac.kr", "01000000000", "컴퓨터소프트웨어학부", LocalDateTime.now());
        when(staffApplicationRepository.findDetailById(500)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> staffApplicationService.getAdminDetail(RECRUITMENT_ID, 500))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_NOT_FOUND);
    }
}
