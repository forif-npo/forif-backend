package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@Schema(description = "스터디 수강 신청서 수정 요청")
public record UserApplyUpdateRequest(
    @Schema(description = "변경할 스터디 ID", example = "1")
    @NotNull
    Integer studyId,

    @Schema(description = "수정할 지원 동기 (50자 이상 500자 이하)", example = "수정된 지원 동기입니다...")
    @NotBlank
    @Length(min = 50, max = 500, message = "지원 사유는 50자 이상 500자 이내로 작성해주세요.")
    String applyReason,

    @Schema(description = "수정할 지원 순위 (1 = 1순위, 2 = 2순위)", example = "1")
    @NotNull
    @Min(value = 1, message = "지원 순위는 1 또는 2만 가능합니다.")
    @Max(value = 2, message = "지원 순위는 1 또는 2만 가능합니다.")
    Integer priority
) {
}