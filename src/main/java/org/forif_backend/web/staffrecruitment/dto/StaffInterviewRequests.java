package org.forif_backend.web.staffrecruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.forif_backend.domain.staffrecruitment.StaffInterviewStatus;

/** 면접 기록 요청. */
public final class StaffInterviewRequests {

    private StaffInterviewRequests() {
    }

    /** 합격이면 지원자의 1지망·2지망 중 배정할 모집 부서를 함께 보낸다. 지망이 하나면 생략할 수 있다. */
    public record StatusRequest(@NotNull StaffInterviewStatus status, Integer departmentId) {
    }

    /** 빈 답변을 보내면 적어 둔 답변을 지운다. */
    public record AnswerRequest(@Size(max = 10000) String answer) {
    }

    public record ExtraQuestionCreateRequest(@NotBlank @Size(max = 500) String content) {
    }

    /** 보낸 필드만 바꾼다. */
    public record ExtraQuestionUpdateRequest(
            @Size(max = 500) String content,
            @Size(max = 10000) String answer
    ) {
    }
}
