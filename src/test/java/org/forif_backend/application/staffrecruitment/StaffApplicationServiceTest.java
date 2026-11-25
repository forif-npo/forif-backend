package org.forif_backend.application.staffrecruitment;

import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationDetail;
import org.forif_backend.application.staffrecruitment.dto.SubmitStaffApplicationCommand;
import org.forif_backend.application.staffrecruitment.dto.SubmitStaffApplicationCommand.AnswerCommand;
import org.forif_backend.application.staffrecruitment.dto.SubmitStaffApplicationCommand.ChoiceCommand;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationAnswer;
import org.forif_backend.domain.staffrecruitment.StaffApplicationChoice;
import org.forif_backend.domain.staffrecruitment.StaffApplicationRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentDepartmentSpec;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionSpec;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlan;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlanRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewScheduleRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewRecordRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewAssignment;
import org.forif_backend.domain.staffrecruitment.StaffInterviewRecord;
import org.forif_backend.domain.staffrecruitment.StaffInterviewStatus;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
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
    private StaffInterviewPlanRepository staffInterviewPlanRepository;

    @Mock
    private StaffInterviewScheduleRepository staffInterviewScheduleRepository;

    @Mock
    private StaffInterviewRecordRepository staffInterviewRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyUserRepository studyUserRepository;

    @Mock
    private FilePort filePort;

    @InjectMocks
    private StaffApplicationService staffApplicationService;

    @Test
    void 본인_면접_요약은_배정과_결과가_없으면_비어_있다() {
        var detail = new StaffApplicationDetail(7, RECRUITMENT_ID, 1L, "Applicant",
                "applicant@example.com", null, null, List.of(),
                org.forif_backend.domain.staffrecruitment.StaffApplicationStatus.SUBMITTED,
                LocalDateTime.now(), List.of(), List.of());

        var summary = staffApplicationService.getInterviewSummary(detail);

        assertThat(summary.date()).isNull();
        assertThat(summary.startMinute()).isNull();
        assertThat(summary.slotMinutes()).isNull();
        assertThat(summary.status()).isNull();
    }

    @Test
    void 본인_면접_요약은_배정시각과_결과만_담는다() {
        var detail = new StaffApplicationDetail(7, RECRUITMENT_ID, 1L, "Applicant",
                "applicant@example.com", null, null, List.of(),
                org.forif_backend.domain.staffrecruitment.StaffApplicationStatus.SUBMITTED,
                LocalDateTime.now(), List.of(), List.of());
        var assignment = mock(StaffInterviewAssignment.class);
        var plan = mock(StaffInterviewPlan.class);
        var record = mock(StaffInterviewRecord.class);
        when(staffInterviewScheduleRepository.findAssignmentByApplicationId(7)).thenReturn(Optional.of(assignment));
        when(staffInterviewPlanRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.of(plan));
        when(staffInterviewRecordRepository.findRecordByApplicationId(7)).thenReturn(Optional.of(record));
        when(assignment.getSlotDate()).thenReturn(LocalDate.of(2026, 11, 26));
        when(assignment.getStartMinute()).thenReturn(960);
        when(plan.getSlotMinutes()).thenReturn(30);
        when(record.getStatus()).thenReturn(StaffInterviewStatus.PASSED);

        var summary = staffApplicationService.getInterviewSummary(detail);

        assertThat(summary.date()).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(summary.startMinute()).isEqualTo(960);
        assertThat(summary.slotMinutes()).isEqualTo(30);
        assertThat(summary.status()).isEqualTo(StaffInterviewStatus.PASSED);
    }

    private final User applicant =
            User.createUser(APPLICANT_ID, "홍길동", "hong@hanyang.ac.kr", "01000000000", "컴퓨터소프트웨어학부");

    private StaffRecruitment openRecruitment() {
        LocalDateTime now = LocalDateTime.now();
        return recruitment(now.minusDays(1), now.plusDays(1));
    }

    private StaffRecruitment recruitment(LocalDateTime startsAt, LocalDateTime endsAt) {
        StaffRecruitment recruitment =
                StaffRecruitment.create(2027, 1, "2027-1 운영진 모집", null, null, startsAt, endsAt);
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);

        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 1, true),
                new StaffRecruitmentDepartmentSpec(null, "개발팀", null, 2, true),
                new StaffRecruitmentDepartmentSpec(null, "디자인팀", null, 3, false)), false);
        List<Integer> departmentIds = List.of(PLANNING_TEAM, DEV_TEAM, CLOSED_TEAM);
        for (int i = 0; i < departmentIds.size(); i++) {
            ReflectionTestUtils.setField(recruitment.getDepartments().get(i), "id", departmentIds.get(i));
        }

        recruitment.mergeQuestions(List.of(new StaffRecruitmentQuestionSpec(
                null, "지원 동기", null, StaffRecruitmentQuestionType.LONG_TEXT, true, 100, 1)), false);
        ReflectionTestUtils.setField(recruitment.getQuestions().get(0), "id", MOTIVE_QUESTION);

        recruitment.publish();
        return recruitment;
    }

    private SubmitStaffApplicationCommand command(List<ChoiceCommand> choices, List<AnswerCommand> answers) {
        return new SubmitStaffApplicationCommand(choices, answers, List.of());
    }

    private List<ChoiceCommand> firstChoiceOnly() {
        return List.of(new ChoiceCommand(PLANNING_TEAM, 1));
    }

    private List<AnswerCommand> validAnswers() {
        return List.of(new AnswerCommand(MOTIVE_QUESTION, "함께 운영하고 싶습니다."));
    }

    private StaffApplication existingApplication(StaffRecruitment recruitment, String answer) {
        StaffApplication application = StaffApplication.submit(recruitment, applicant, "홍길동",
                "hong@hanyang.ac.kr", "01000000000", "컴퓨터소프트웨어학부", LocalDateTime.now());
        application.addChoice(StaffApplicationChoice.create(recruitment.getDepartments().get(0), 1));
        application.addAnswer(StaffApplicationAnswer.create(recruitment.getQuestions().get(0), answer));
        when(staffApplicationRepository.findForMutation(RECRUITMENT_ID, APPLICANT_ID))
                .thenReturn(Optional.of(application));
        return application;
    }

    private void givenOpenRecruitment() {
        when(staffRecruitmentRepository.findDetailById(RECRUITMENT_ID)).thenReturn(Optional.of(openRecruitment()));
    }

    @Test
    void 본인_지원서_수정_가능_여부는_접수_상태와_수강_이력으로_판정한다() {
        when(staffRecruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(Optional.of(openRecruitment()));
        givenStudyHistory();
        assertThat(staffApplicationService.canModifyMine(RECRUITMENT_ID, APPLICANT_ID)).isTrue();

        when(studyUserRepository.existsByUserId(APPLICANT_ID))
                .thenReturn(false);
        assertThat(staffApplicationService.canModifyMine(RECRUITMENT_ID, APPLICANT_ID)).isFalse();
    }

    @Test
    void 모집이_마감되면_수강_이력이_있어도_수정할_수_없다() {
        LocalDateTime now = LocalDateTime.now();
        when(staffRecruitmentRepository.findById(RECRUITMENT_ID))
                .thenReturn(Optional.of(recruitment(now.minusDays(10), now.minusDays(1))));

        assertThat(staffApplicationService.canModifyMine(RECRUITMENT_ID, APPLICANT_ID)).isFalse();
    }

    private void givenQuestionType(StaffRecruitmentQuestionType type) {
        StaffRecruitment recruitment = openRecruitment();
        ReflectionTestUtils.setField(recruitment.getQuestions().get(0), "type", type);
        when(staffRecruitmentRepository.findDetailById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
    }

    @Test
    void 필수_파일_문항은_파일이_없으면_제출할_수_없다() {
        givenQuestionType(StaffRecruitmentQuestionType.FILE);
        givenStudyHistory();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, "")))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        verify(staffApplicationRepository, never()).save(any());
    }

    @Test
    void 파일_문항은_5메가바이트_초과_파일을_거부한다() {
        givenQuestionType(StaffRecruitmentQuestionType.FILE);
        givenStudyHistory();
        givenApplicantLoaded();
        MockMultipartFile file = new MockMultipartFile("file_41", "portfolio.pdf", "application/pdf",
                new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, ""))),
                Map.of(MOTIVE_QUESTION, file)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FILE_ATTACHMENT);
        verify(filePort, never()).uploadFile(any(), any());
    }

    @Test
    void 파일_답변은_비공개_경로에_저장하고_상세에는_파일명만_표시한다() {
        givenQuestionType(StaffRecruitmentQuestionType.FILE);
        givenStudyHistory();
        givenApplicantLoaded();
        MockMultipartFile file = new MockMultipartFile("file_41", "portfolio.pdf", "application/pdf", new byte[]{1});
        String key = "staff-applications/12/123e4567-e89b-12d3-a456-426614174000-portfolio.pdf";
        when(filePort.uploadFile(eq(file), eq("staff-applications/12"))).thenReturn(key);
        when(staffApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(filePort.downloadBytes(key)).thenReturn(new byte[]{1});

        StaffApplicationDetail detail = staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, ""))),
                Map.of(MOTIVE_QUESTION, file));

        assertThat(detail.answers().get(0).answer()).isEqualTo(key);
        assertThat(StaffApplicationResponse.DetailResponse.from(detail).answers().get(0).answer())
                .isEqualTo("portfolio.pdf");
        assertThat(staffApplicationService.downloadAttachment(detail, MOTIVE_QUESTION).bytes())
                .containsExactly((byte) 1);
    }

    @Test
    void 저장된_파일이_사라졌다면_404를_반환한다() {
        String key = "staff-applications/12/123e4567-e89b-12d3-a456-426614174000-portfolio.pdf";
        StaffApplicationDetail detail = new StaffApplicationDetail(
                7, RECRUITMENT_ID, APPLICANT_ID, "Applicant", "applicant@example.com", null, null,
                List.of(), null, LocalDateTime.now(),
                List.of(new StaffApplicationDetail.AnswerInfo(
                        MOTIVE_QUESTION, "파일", StaffRecruitmentQuestionType.FILE, key)),
                List.of());
        when(filePort.downloadBytes(key)).thenThrow(new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT));

        assertThatThrownBy(() -> staffApplicationService.downloadAttachment(detail, MOTIVE_QUESTION))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void 링크_문항은_http_스킴만_허용한다() {
        givenQuestionType(StaffRecruitmentQuestionType.LINK);
        givenStudyHistory();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, "javascript:alert(1)")))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
    }

    @Test
    void 첨부파일_문항은_링크_또는_파일_하나를_제출할_수_있다() {
        givenQuestionType(StaffRecruitmentQuestionType.ATTACHMENT);
        givenStudyHistory();
        givenApplicantLoaded();
        when(staffApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffApplicationDetail linked = staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, "https://example.com/portfolio"))));
        assertThat(linked.answers().get(0).questionType()).isEqualTo(StaffRecruitmentQuestionType.LINK);
        assertThat(linked.answers().get(0).answer()).isEqualTo("https://example.com/portfolio");

        MockMultipartFile file = new MockMultipartFile("file_41", "portfolio.pdf", "application/pdf", new byte[]{1});
        String key = "staff-applications/12/portfolio.pdf";
        when(filePort.uploadFile(eq(file), eq("staff-applications/12"))).thenReturn(key);
        StaffApplicationDetail uploaded = staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, ""))),
                Map.of(MOTIVE_QUESTION, file));
        assertThat(uploaded.answers().get(0).questionType()).isEqualTo(StaffRecruitmentQuestionType.FILE);
        assertThat(StaffApplicationResponse.DetailResponse.from(uploaded).answers().get(0).answer())
                .isEqualTo("portfolio.pdf");
    }

    @Test
    void 첨부파일_문항은_링크와_파일을_동시에_받지_않는다() {
        givenQuestionType(StaffRecruitmentQuestionType.ATTACHMENT);
        givenStudyHistory();
        givenApplicantLoaded();
        MockMultipartFile file = new MockMultipartFile("file_41", "portfolio.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, "https://example.com"))),
                Map.of(MOTIVE_QUESTION, file)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        verify(filePort, never()).uploadFile(any(), any());
    }

    @Test
    void 첨부파일_문항의_기존_파일을_링크로_바꾸면_파일을_정리한다() {
        StaffRecruitment recruitment = openRecruitment();
        ReflectionTestUtils.setField(recruitment.getQuestions().get(0), "type", StaffRecruitmentQuestionType.ATTACHMENT);
        String oldKey = "staff-applications/12/old.pdf";
        StaffApplication application = existingApplication(recruitment, oldKey);
        ReflectionTestUtils.setField(application.getAnswers().get(0), "questionType", StaffRecruitmentQuestionType.FILE);
        givenStudyHistory();

        StaffApplicationDetail updated = staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, "https://example.com/new"))),
                Map.of(), Set.of(MOTIVE_QUESTION));

        assertThat(updated.answers().get(0).questionType()).isEqualTo(StaffRecruitmentQuestionType.LINK);
        assertThat(updated.answers().get(0).answer()).isEqualTo("https://example.com/new");
        verify(filePort).deleteFile(oldKey);
    }

    private void givenStudyHistory() {
        when(studyUserRepository.existsByUserId(APPLICANT_ID)).thenReturn(true);
    }

    private void givenApplicantLoaded() {
        when(staffApplicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, APPLICANT_ID))
                .thenReturn(false);
        when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
    }

    private static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);
    private static final LocalDate DEC_4 = LocalDate.of(2026, 12, 4);

    private StaffInterviewPlan givenInterviewPlan(boolean required) {
        StaffInterviewPlan plan = StaffInterviewPlan.create(openRecruitment(), 9, 22, 30, required,
                "면접 가능 시간을 표시해주세요.", List.of(DEC_1, DEC_4));
        when(staffInterviewPlanRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.of(plan));
        return plan;
    }

    private SubmitStaffApplicationCommand commandWithSlots(List<SubmitStaffApplicationCommand.InterviewSlotCommand> slots) {
        return new SubmitStaffApplicationCommand(firstChoiceOnly(), validAnswers(), slots);
    }

    @Test
    void 면접_가능_시간을_고르면_지원서에_함께_저장한다() {
        givenOpenRecruitment();
        givenStudyHistory();
        givenApplicantLoaded();
        givenInterviewPlan(true);
        when(staffApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffApplicationDetail detail = staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                commandWithSlots(List.of(
                        new SubmitStaffApplicationCommand.InterviewSlotCommand(DEC_1, 9 * 60),
                        new SubmitStaffApplicationCommand.InterviewSlotCommand(DEC_4, 21 * 60 + 30))));

        assertThat(detail.interviewSlots()).hasSize(2);
        assertThat(detail.interviewSlots().get(0).date()).isEqualTo(DEC_1);
        assertThat(detail.interviewSlots().get(0).startMinute()).isEqualTo(9 * 60);
    }

    @Test
    void 같은_칸을_두_번_보내면_한_번만_저장한다() {
        givenOpenRecruitment();
        givenStudyHistory();
        givenApplicantLoaded();
        givenInterviewPlan(true);
        when(staffApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffApplicationDetail detail = staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                commandWithSlots(List.of(
                        new SubmitStaffApplicationCommand.InterviewSlotCommand(DEC_1, 10 * 60),
                        new SubmitStaffApplicationCommand.InterviewSlotCommand(DEC_1, 10 * 60))));

        assertThat(detail.interviewSlots()).hasSize(1);
    }

    @Test
    void 설정_범위_밖의_칸은_거절한다() {
        givenOpenRecruitment();
        givenStudyHistory();
        givenApplicantLoaded();
        givenInterviewPlan(true);

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                commandWithSlots(List.of(
                        new SubmitStaffApplicationCommand.InterviewSlotCommand(DEC_1, 22 * 60)))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_SLOT_INVALID);
        verify(staffApplicationRepository, never()).save(any());
    }

    @Test
    void 필수인데_한_칸도_고르지_않으면_거절한다() {
        givenOpenRecruitment();
        givenStudyHistory();
        givenApplicantLoaded();
        givenInterviewPlan(true);

        assertThatThrownBy(() -> staffApplicationService.submit(
                RECRUITMENT_ID, APPLICANT_ID, commandWithSlots(List.of())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_SLOT_INVALID);
    }

    @Test
    void 면접_일정이_없는_회차에_칸을_보내면_거절한다() {
        givenOpenRecruitment();
        givenStudyHistory();
        givenApplicantLoaded();
        when(staffInterviewPlanRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                commandWithSlots(List.of(
                        new SubmitStaffApplicationCommand.InterviewSlotCommand(DEC_1, 9 * 60)))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_SLOT_INVALID);
    }

    @Test
    void 스터디_수강_이력이_없으면_제출할_수_없다() {
        givenOpenRecruitment();

        assertThatThrownBy(() -> staffApplicationService.submit(
                RECRUITMENT_ID, APPLICANT_ID, command(firstChoiceOnly(), validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_ELIGIBILITY_REQUIRED);
        verify(staffApplicationRepository, never()).save(any());
    }

    @Test
    void 같은_회차에_두_번_제출할_수_없다() {
        givenOpenRecruitment();
        givenStudyHistory();
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
        givenStudyHistory();
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
        givenStudyHistory();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(List.of(new ChoiceCommand(PLANNING_TEAM, 2)), validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
    }

    @Test
    void 같은_부서를_두_지망에_넣을_수_없다() {
        givenOpenRecruitment();
        givenStudyHistory();
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
        givenStudyHistory();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(List.of(new ChoiceCommand(CLOSED_TEAM, 1)), validAnswers())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
    }

    @Test
    void 필수_문항에_답하지_않으면_제출할_수_없다() {
        givenOpenRecruitment();
        givenStudyHistory();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
    }

    @Test
    void 문항의_최대_길이를_넘으면_제출할_수_없다() {
        givenOpenRecruitment();
        givenStudyHistory();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, "가".repeat(101))))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
    }

    @Test
    void 다른_회차의_문항에는_답할_수_없다() {
        givenOpenRecruitment();
        givenStudyHistory();
        givenApplicantLoaded();

        assertThatThrownBy(() -> staffApplicationService.submit(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(9999, "다른 회차 문항")))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
    }

    @Test
    void 제출하면_그_시점의_개인정보와_지망_답변을_저장한다() {
        givenOpenRecruitment();
        givenStudyHistory();
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
    void 본인_지원_이력은_회차와_제출일만_반환한다() {
        StaffRecruitment recruitment = openRecruitment();
        StaffApplication application = StaffApplication.submit(recruitment, applicant, "홍길동",
                "hong@hanyang.ac.kr", "01000000000", "컴퓨터소프트웨어학부", LocalDateTime.now());
        ReflectionTestUtils.setField(application, "id", 500);
        when(staffApplicationRepository.findByApplicantId(APPLICANT_ID)).thenReturn(List.of(application));

        var history = staffApplicationService.getMyHistory(APPLICANT_ID);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).applicationId()).isEqualTo(500);
        assertThat(history.get(0).recruitmentId()).isEqualTo(RECRUITMENT_ID);
        assertThat(history.get(0).actYear()).isEqualTo(2027);
        assertThat(history.get(0).actSemester()).isEqualTo(1);
        verify(staffApplicationRepository).findByApplicantId(APPLICANT_ID);
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

    @Test
    void 접수_중인_본인_지원서는_지망과_답변을_수정한다() {
        StaffApplication application = existingApplication(openRecruitment(), "기존 답변");
        givenStudyHistory();

        StaffApplicationDetail result = staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(List.of(new ChoiceCommand(DEV_TEAM, 1)), validAnswers()), Map.of(), Set.of());

        assertThat(result.appliedDepartments().get(0).name()).isEqualTo("개발팀");
        assertThat(result.answers().get(0).answer()).isEqualTo("함께 운영하고 싶습니다.");
        assertThat(application.getAnswers()).hasSize(1);
        verify(staffApplicationRepository).flush();
    }

    @Test
    void 수정_요청에서_선택_문항을_누락해도_기존_답변을_지우지_않는다() {
        StaffRecruitment recruitment = openRecruitment();
        ReflectionTestUtils.setField(recruitment.getQuestions().get(0), "required", false);
        StaffApplication application = existingApplication(recruitment, "기존 답변");
        givenStudyHistory();

        assertThatThrownBy(() -> staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of()), Map.of(), Set.of()))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        assertThat(application.getAnswers().get(0).getAnswer()).isEqualTo("기존 답변");
        verify(staffApplicationRepository, never()).flush();
    }

    @Test
    void 스터디_수강_이력이_없으면_수정은_막고_취소는_허용한다() {
        StaffApplication application = existingApplication(openRecruitment(), "기존 답변");

        assertThatThrownBy(() -> staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), validAnswers()), Map.of(), Set.of()))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_ELIGIBILITY_REQUIRED);
        assertThat(application.getAnswers().get(0).getAnswer()).isEqualTo("기존 답변");

        staffApplicationService.cancel(RECRUITMENT_ID, APPLICANT_ID);
        verify(staffApplicationRepository).delete(application);
    }

    @Test
    void 잘못된_수정_요청은_기존_지원서를_바꾸지_않는다() {
        StaffApplication application = existingApplication(openRecruitment(), "기존 답변");
        givenStudyHistory();

        assertThatThrownBy(() -> staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(List.of(new ChoiceCommand(CLOSED_TEAM, 1)), validAnswers()), Map.of(), Set.of()))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
        assertThat(application.getAnswers().get(0).getAnswer()).isEqualTo("기존 답변");
        verify(staffApplicationRepository, never()).flush();
    }

    @Test
    void 기존_파일은_유지하고_새_파일로_교체하면_이전_파일을_정리한다() {
        StaffRecruitment recruitment = openRecruitment();
        ReflectionTestUtils.setField(recruitment.getQuestions().get(0), "type", StaffRecruitmentQuestionType.FILE);
        String oldKey = "staff-applications/12/old.pdf";
        existingApplication(recruitment, oldKey);
        givenStudyHistory();
        StaffApplicationDetail retained = staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, ""))), Map.of(), Set.of());
        assertThat(retained.answers().get(0).answer()).isEqualTo(oldKey);
        verify(filePort, never()).deleteFile(any());

        MockMultipartFile replacement = new MockMultipartFile("file_41", "new.pdf", "application/pdf", new byte[]{1});
        String newKey = "staff-applications/12/new.pdf";
        when(filePort.uploadFile(eq(replacement), eq("staff-applications/12"))).thenReturn(newKey);
        StaffApplicationDetail updated = staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, ""))),
                Map.of(MOTIVE_QUESTION, replacement), Set.of());
        assertThat(updated.answers().get(0).answer()).isEqualTo(newKey);
        verify(filePort).deleteFile(oldKey);
    }

    @Test
    void 선택_파일을_제거하면_저장된_파일도_정리한다() {
        StaffRecruitment recruitment = openRecruitment();
        ReflectionTestUtils.setField(recruitment.getQuestions().get(0), "type", StaffRecruitmentQuestionType.FILE);
        ReflectionTestUtils.setField(recruitment.getQuestions().get(0), "required", false);
        String oldKey = "staff-applications/12/old.pdf";
        existingApplication(recruitment, oldKey);
        givenStudyHistory();

        StaffApplicationDetail updated = staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), List.of(new AnswerCommand(MOTIVE_QUESTION, ""))),
                Map.of(), Set.of(MOTIVE_QUESTION));

        assertThat(updated.answers().get(0).answer()).isBlank();
        verify(filePort).deleteFile(oldKey);
    }

    @Test
    void 접수_마감_후에는_수정과_취소를_거부한다() {
        LocalDateTime now = LocalDateTime.now();
        StaffRecruitment closed = recruitment(now.minusDays(10), now.minusDays(1));
        StaffApplication application = StaffApplication.submit(closed, applicant, "홍길동",
                "hong@hanyang.ac.kr", null, null, now.minusDays(5));
        when(staffApplicationRepository.findForMutation(RECRUITMENT_ID, APPLICANT_ID))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), validAnswers()), Map.of(), Set.of()))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_NOT_OPEN);
        assertThatThrownBy(() -> staffApplicationService.cancel(RECRUITMENT_ID, APPLICANT_ID))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_NOT_OPEN);
        verify(staffApplicationRepository, never()).delete(any());
    }

    @Test
    void 본인_지원서가_없으면_수정과_취소를_거부한다() {
        when(staffApplicationRepository.findForMutation(RECRUITMENT_ID, APPLICANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffApplicationService.update(RECRUITMENT_ID, APPLICANT_ID,
                command(firstChoiceOnly(), validAnswers()), Map.of(), Set.of()))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_NOT_FOUND);
        assertThatThrownBy(() -> staffApplicationService.cancel(RECRUITMENT_ID, APPLICANT_ID))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_NOT_FOUND);
    }

    @Test
    void 취소하면_지원서와_첨부파일을_삭제한다() {
        StaffRecruitment recruitment = openRecruitment();
        ReflectionTestUtils.setField(recruitment.getQuestions().get(0), "type", StaffRecruitmentQuestionType.FILE);
        String key = "staff-applications/12/portfolio.pdf";
        StaffApplication application = existingApplication(recruitment, key);

        staffApplicationService.cancel(RECRUITMENT_ID, APPLICANT_ID);

        verify(staffApplicationRepository).delete(application);
        verify(filePort).deleteFile(key);
    }
}
