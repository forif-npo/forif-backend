package org.forif_backend.application.staffrecruitment;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewBoardDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewSheetDetail;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffInterviewBoardServiceTest {

    private static final Integer RECRUITMENT_ID = 12;
    private static final Integer APPLICATION_ID = 101;
    private static final Integer SW = 31;
    private static final Integer PLANNING = 32;
    private static final Integer DESIGN = 33;

    @Mock private StaffRecruitmentRepository staffRecruitmentRepository;
    @Mock private StaffApplicationRepository staffApplicationRepository;
    @Mock private StaffInterviewPlanRepository staffInterviewPlanRepository;
    @Mock private StaffInterviewScheduleRepository staffInterviewScheduleRepository;
    @Mock private StaffInterviewQuestionRepository staffInterviewQuestionRepository;
    @Mock private StaffInterviewRecordRepository staffInterviewRecordRepository;

    @InjectMocks
    private StaffInterviewBoardService staffInterviewBoardService;

    private StaffRecruitment recruitment;
    private StaffApplication application;
    private StaffInterviewQuestion common;
    private StaffInterviewQuestion swQuestion;
    private StaffInterviewQuestion designQuestion;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        recruitment = StaffRecruitment.create(2026, 2, "2026-2 운영진 모집", null, null,
                now.minusDays(1), now.plusDays(1));
        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "SW팀", null, 1, true),
                new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 2, true),
                new StaffRecruitmentDepartmentSpec(null, "디자인팀", null, 3, true)), false);
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);
        List<Integer> ids = List.of(SW, PLANNING, DESIGN);
        for (int i = 0; i < ids.size(); i++) {
            ReflectionTestUtils.setField(recruitment.getDepartments().get(i), "id", ids.get(i));
        }
        recruitment.publish();

        application = StaffApplication.submit(recruitment, null, "가지원", "a@example.com", null, null, now);
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
        application.addChoice(StaffApplicationChoice.create(recruitment.getDepartments().get(0), 1));
        application.addChoice(StaffApplicationChoice.create(recruitment.getDepartments().get(1), 2));
        recruitment.close("면접", now);

        common = question(1, null, "지원 동기");
        swQuestion = question(2, recruitment.getDepartments().get(0), "써 본 프레임워크");
        designQuestion = question(3, recruitment.getDepartments().get(2), "포트폴리오 설명");

        when(staffRecruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
        when(staffApplicationRepository.findDetailById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(staffInterviewQuestionRepository.findAllByRecruitmentId(RECRUITMENT_ID))
                .thenReturn(List.of(common, swQuestion, designQuestion));
        when(staffInterviewRecordRepository.findAnswersByApplicationId(APPLICATION_ID)).thenReturn(List.of());
        when(staffInterviewRecordRepository.findExtraQuestionsByApplicationId(APPLICATION_ID)).thenReturn(List.of());
        when(staffInterviewRecordRepository.findRecordByApplicationId(APPLICATION_ID)).thenReturn(Optional.empty());
        when(staffInterviewScheduleRepository.findAssignments(RECRUITMENT_ID)).thenReturn(List.of());
        when(staffInterviewPlanRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.empty());
    }

    private StaffInterviewQuestion question(int id, StaffRecruitmentDepartment department, String content) {
        StaffInterviewQuestion question = StaffInterviewQuestion.create(recruitment, department, content, id);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    @Test
    void 기록지는_공통_질문과_그_지원자의_1지망_2지망_부서_질문만_담는다() {
        StaffInterviewSheetDetail sheet = staffInterviewBoardService.getSheet(RECRUITMENT_ID, APPLICATION_ID);

        assertThat(sheet.status()).isEqualTo(StaffInterviewStatus.BEFORE);
        assertThat(sheet.commonQuestions()).extracting(StaffInterviewSheetDetail.QuestionAnswer::content)
                .containsExactly("지원 동기");
        assertThat(sheet.firstDepartment().departmentName()).isEqualTo("SW팀");
        assertThat(sheet.firstDepartment().questions()).extracting(StaffInterviewSheetDetail.QuestionAnswer::content)
                .containsExactly("써 본 프레임워크");
        assertThat(sheet.secondDepartment().departmentName()).isEqualTo("기획팀");
        // 기획팀 질문이 없으면 빈 목록이고, 지원하지 않은 디자인팀 질문은 보이지 않는다.
        assertThat(sheet.secondDepartment().questions()).isEmpty();
    }

    @Test
    void 처음_상태를_바꾸면_기록을_만든다() {
        staffInterviewBoardService.changeStatus(RECRUITMENT_ID, APPLICATION_ID, StaffInterviewStatus.DONE);

        ArgumentCaptor<StaffInterviewRecord> saved = ArgumentCaptor.forClass(StaffInterviewRecord.class);
        verify(staffInterviewRecordRepository).saveRecord(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(StaffInterviewStatus.DONE);
    }

    @Test
    void 보드에서는_어느_상태로든_옮길_수_있다() {
        // 합격으로 잘못 옮긴 카드를 면접 전으로 되돌리는 것도 끌어서 한다.
        StaffInterviewRecord record = StaffInterviewRecord.create(application, StaffInterviewStatus.PASSED);
        when(staffInterviewRecordRepository.findRecordByApplicationId(APPLICATION_ID)).thenReturn(Optional.of(record));

        staffInterviewBoardService.changeStatus(RECRUITMENT_ID, APPLICATION_ID, StaffInterviewStatus.BEFORE);

        assertThat(record.getStatus()).isEqualTo(StaffInterviewStatus.BEFORE);
        verify(staffInterviewRecordRepository, never()).saveRecord(any());
    }

    @Test
    void 지원하지_않은_부서의_질문에는_답을_적을_수_없다() {
        when(staffInterviewQuestionRepository.findById(3)).thenReturn(Optional.of(designQuestion));

        assertThatThrownBy(() -> staffInterviewBoardService.saveAnswer(RECRUITMENT_ID, APPLICATION_ID, 3, "답"))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 답변을_처음_적으면_새로_만들고_비우면_지운다() {
        when(staffInterviewQuestionRepository.findById(1)).thenReturn(Optional.of(common));

        staffInterviewBoardService.saveAnswer(RECRUITMENT_ID, APPLICATION_ID, 1, "웹을 만들고 싶습니다.");
        verify(staffInterviewRecordRepository).saveAnswer(any());

        StaffInterviewAnswer saved = StaffInterviewAnswer.create(application, common, "웹을 만들고 싶습니다.");
        when(staffInterviewRecordRepository.findAnswer(APPLICATION_ID, 1)).thenReturn(Optional.of(saved));
        staffInterviewBoardService.saveAnswer(RECRUITMENT_ID, APPLICATION_ID, 1, "   ");
        verify(staffInterviewRecordRepository).deleteAnswer(saved);
    }

    @Test
    void 기타_질문은_그_지원자_뒤에_순서대로_붙는다() {
        StaffInterviewExtraQuestion first = StaffInterviewExtraQuestion.create(application, "첫 기타 질문", 1);
        when(staffInterviewRecordRepository.findExtraQuestionsByApplicationId(APPLICATION_ID)).thenReturn(List.of(first));

        staffInterviewBoardService.addExtraQuestion(RECRUITMENT_ID, APPLICATION_ID, "두 번째 질문");

        ArgumentCaptor<StaffInterviewExtraQuestion> saved = ArgumentCaptor.forClass(StaffInterviewExtraQuestion.class);
        verify(staffInterviewRecordRepository).saveExtraQuestion(saved.capture());
        assertThat(saved.getValue().getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void 다른_지원자의_기타_질문은_고칠_수_없다() {
        StaffApplication other = StaffApplication.submit(recruitmentOpenCopy(), null, "나지원", "b@example.com",
                null, null, LocalDateTime.now());
        ReflectionTestUtils.setField(other, "id", 999);
        when(staffInterviewRecordRepository.findExtraQuestionById(5))
                .thenReturn(Optional.of(StaffInterviewExtraQuestion.create(other, "남의 질문", 1)));

        assertThatThrownBy(() -> staffInterviewBoardService.updateExtraQuestion(
                RECRUITMENT_ID, APPLICATION_ID, 5, null, "답"))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_EXTRA_QUESTION_NOT_FOUND);
    }

    @Test
    void 마감_전에는_기록을_바꿀_수_없다() {
        LocalDateTime now = LocalDateTime.now();
        StaffRecruitment open = StaffRecruitment.create(2026, 2, "모집", null, null, now.minusDays(1), now.plusDays(1));
        open.publish();
        when(staffRecruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(Optional.of(open));

        assertThatThrownBy(() -> staffInterviewBoardService.changeStatus(
                RECRUITMENT_ID, APPLICATION_ID, StaffInterviewStatus.DONE))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
    }

    @Test
    void 보드는_기록이_없는_지원자를_면접_전으로_두고_면접_시각_순으로_놓는다() {
        StaffApplication later = StaffApplication.submit(recruitmentOpenCopy(), null, "늦은지원", "c@example.com",
                null, null, LocalDateTime.now());
        ReflectionTestUtils.setField(later, "recruitment", recruitment);
        ReflectionTestUtils.setField(later, "id", 102);
        when(staffApplicationRepository.findAllWithChoicesByRecruitmentId(RECRUITMENT_ID))
                .thenReturn(List.of(application, later));
        when(staffInterviewRecordRepository.findRecordsByRecruitmentId(RECRUITMENT_ID)).thenReturn(List.of());
        when(staffInterviewScheduleRepository.findAssignments(RECRUITMENT_ID)).thenReturn(List.of(
                StaffInterviewAssignment.create(recruitment, later, LocalDate.of(2026, 12, 1), 540),
                StaffInterviewAssignment.create(recruitment, application, LocalDate.of(2026, 12, 2), 540)));

        StaffInterviewBoardDetail board = staffInterviewBoardService.getBoard(RECRUITMENT_ID);

        assertThat(board.cards()).extracting(StaffInterviewBoardDetail.Card::applicantName)
                .containsExactly("늦은지원", "가지원");
        assertThat(board.cards()).allMatch(card -> card.status() == StaffInterviewStatus.BEFORE);
        assertThat(board.cards().get(1).firstDepartment()).isEqualTo("SW팀");
        assertThat(board.cards().get(1).secondDepartment()).isEqualTo("기획팀");
    }

    private StaffRecruitment recruitmentOpenCopy() {
        LocalDateTime now = LocalDateTime.now();
        StaffRecruitment copy = StaffRecruitment.create(2026, 2, "모집", null, null, now.minusDays(1), now.plusDays(1));
        copy.publish();
        return copy;
    }
}
