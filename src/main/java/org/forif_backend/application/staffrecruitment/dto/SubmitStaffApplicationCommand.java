package org.forif_backend.application.staffrecruitment.dto;

import java.util.List;

/**
 * 운영진 지원서 제출 입력.
 * 1차 범위는 즉시 제출만이라 임시저장·수정 입력이 없다.
 */
public record SubmitStaffApplicationCommand(
        List<ChoiceCommand> departmentChoices,
        List<AnswerCommand> answers
) {

    public record ChoiceCommand(Integer departmentId, int priority) {
    }

    public record AnswerCommand(Integer questionId, String answer) {
    }
}
