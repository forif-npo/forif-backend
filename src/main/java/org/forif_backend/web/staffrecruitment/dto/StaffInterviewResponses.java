package org.forif_backend.web.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewBoardDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail.Slot;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewSheetDetail;
import org.forif_backend.domain.staffrecruitment.StaffInterviewStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 면접 관리 응답. 면접 시각은 날짜 + 시작 분(자정 기준)이고 길이는 slot_minutes다.
 */
public final class StaffInterviewResponses {

    private StaffInterviewResponses() {
    }

    public record SlotResponse(LocalDate date, int startMinute) {
        static SlotResponse from(Slot slot) {
            return slot == null ? null : new SlotResponse(slot.date(), slot.startMinute());
        }
    }

    public record CardResponse(
            Integer applicationId,
            String applicantName,
            SlotResponse assignedSlot,
            String firstDepartment,
            String secondDepartment,
            StaffInterviewStatus status
    ) {
    }

    public record BoardResponse(Integer slotMinutes, List<CardResponse> cards) {
        public static BoardResponse from(StaffInterviewBoardDetail detail) {
            return new BoardResponse(detail.slotMinutes(), detail.cards().stream()
                    .map(card -> new CardResponse(card.applicationId(), card.applicantName(),
                            SlotResponse.from(card.assignedSlot()), card.firstDepartment(),
                            card.secondDepartment(), card.status()))
                    .toList());
        }
    }

    public record QuestionAnswerResponse(Integer questionId, String content, String answer) {
    }

    public record DepartmentQuestionsResponse(Integer departmentId, String departmentName,
                                              List<QuestionAnswerResponse> questions) {
    }

    public record ExtraQuestionResponse(Integer extraQuestionId, String content, String answer) {
    }

    public record SheetResponse(
            Integer applicationId,
            String applicantName,
            StaffInterviewStatus status,
            SlotResponse assignedSlot,
            Integer slotMinutes,
            List<QuestionAnswerResponse> commonQuestions,
            DepartmentQuestionsResponse firstDepartment,
            DepartmentQuestionsResponse secondDepartment,
            List<ExtraQuestionResponse> extraQuestions
    ) {
        public static SheetResponse from(StaffInterviewSheetDetail detail) {
            return new SheetResponse(
                    detail.applicationId(),
                    detail.applicantName(),
                    detail.status(),
                    SlotResponse.from(detail.assignedSlot()),
                    detail.slotMinutes(),
                    answers(detail.commonQuestions()),
                    department(detail.firstDepartment()),
                    department(detail.secondDepartment()),
                    detail.extraQuestions().stream()
                            .map(extra -> new ExtraQuestionResponse(extra.extraQuestionId(), extra.content(), extra.answer()))
                            .toList());
        }

        private static List<QuestionAnswerResponse> answers(List<StaffInterviewSheetDetail.QuestionAnswer> items) {
            return items.stream()
                    .map(item -> new QuestionAnswerResponse(item.questionId(), item.content(), item.answer()))
                    .toList();
        }

        private static DepartmentQuestionsResponse department(StaffInterviewSheetDetail.DepartmentQuestions value) {
            return value == null ? null : new DepartmentQuestionsResponse(
                    value.departmentId(), value.departmentName(), answers(value.questions()));
        }
    }
}
