package org.forif_backend.application.staffrecruitment.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 운영진 지원서의 지망·답변·면접 가능 시간 입력. 제출과 수정에 같은 검증을 적용한다.
 */
public record SubmitStaffApplicationCommand(
        List<ChoiceCommand> departmentChoices,
        List<AnswerCommand> answers,
        List<InterviewSlotCommand> interviewSlots
) {

    public record ChoiceCommand(Integer departmentId, int priority) {
    }

    public record AnswerCommand(Integer questionId, String answer) {
    }

    /** 면접 가능 시간 한 칸. {@code startMinute}는 자정부터의 분이다. 09:30이면 570이다. */
    public record InterviewSlotCommand(LocalDate date, int startMinute) {
    }
}
