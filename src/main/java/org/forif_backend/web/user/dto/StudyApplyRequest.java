package org.forif_backend.web.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record StudyApplyRequest(
    @NotNull
    Integer primaryStudyId,

    @NotBlank
    @Length(min = 50, max = 500, message = "지원 사유는 50자 이상 500자 이내로 작성해주세요.")
    String primaryStudyApplyReason,

    Integer secondaryStudyId,

    @Length(min = 50, max = 500, message = "지원 사유는 50자 이상 500자 이내로 작성해주세요.")
    String secondaryStudyApplyReason
) {
}
