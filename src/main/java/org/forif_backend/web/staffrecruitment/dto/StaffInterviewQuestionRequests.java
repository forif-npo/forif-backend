package org.forif_backend.web.staffrecruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class StaffInterviewQuestionRequests {

    private StaffInterviewQuestionRequests() {
    }

    /** departmentId를 비우면 공통 질문이다. */
    public record CreateRequest(
            Integer departmentId,
            @NotBlank @Size(max = 500) String content
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 500) String content
    ) {
    }
}
