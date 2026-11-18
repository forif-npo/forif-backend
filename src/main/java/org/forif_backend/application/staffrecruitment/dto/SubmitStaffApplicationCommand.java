package org.forif_backend.application.staffrecruitment.dto;

import java.util.List;

/**
 * 운영진 지원서의 지망·답변 입력. 제출과 수정에 같은 검증을 적용한다.
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
