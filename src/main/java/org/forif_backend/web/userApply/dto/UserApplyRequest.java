package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@Schema(description = "스터디 수강 신청 요청")
public record UserApplyRequest(
    @Schema(description = "1지망 스터디 ID", example = "1")
    @NotNull
    Integer primaryStudyId,

    @Schema(description = "1지망 스터디 지원 동기 (50자 이상 500자 이하)", example = "이 스터디에 지원하는 이유는...")
    @NotBlank
    @Length(min = 50, max = 500, message = "지원 사유는 50자 이상 500자 이내로 작성해주세요.")
    String primaryStudyApplyReason,

    @Schema(description = "2지망 스터디 ID (선택)", example = "2")
    Integer secondaryStudyId,

    @Schema(description = "2지망 스터디 지원 동기 (50자 이상 500자 이하, 2지망 선택 시 필수)", example = "2지망으로 지원하는 이유는...")
    @Length(min = 50, max = 500, message = "지원 사유는 50자 이상 500자 이내로 작성해주세요.")
    String secondaryStudyApplyReason
) {
}
