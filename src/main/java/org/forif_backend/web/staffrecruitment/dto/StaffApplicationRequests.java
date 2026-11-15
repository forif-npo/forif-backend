package org.forif_backend.web.staffrecruitment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.forif_backend.application.staffrecruitment.dto.SubmitStaffApplicationCommand;

import java.util.List;

/**
 * 운영진 지원서 제출 요청.
 * 지원자 식별은 인증 컨텍스트에서 얻으므로 본문에 사용자 식별값을 받지 않는다.
 */
public final class StaffApplicationRequests {

    private StaffApplicationRequests() {
    }

    public record ChoiceRequest(
            @NotNull Integer departmentId,
            @Min(1) @Max(2) int priority
    ) {
    }

    public record AnswerRequest(
            @NotNull Integer questionId,
            @Size(max = 5000) String answer
    ) {
    }

    public record SubmitRequest(
            @NotEmpty @Valid List<ChoiceRequest> departmentChoices,
            @Valid List<AnswerRequest> answers
    ) {
        public SubmitStaffApplicationCommand toCommand() {
            return new SubmitStaffApplicationCommand(
                    departmentChoices == null ? List.of() : departmentChoices.stream()
                            .map(choice -> new SubmitStaffApplicationCommand.ChoiceCommand(
                                    choice.departmentId(), choice.priority()))
                            .toList(),
                    answers == null ? List.of() : answers.stream()
                            .map(answer -> new SubmitStaffApplicationCommand.AnswerCommand(
                                    answer.questionId(), answer.answer()))
                            .toList());
        }
    }
}
