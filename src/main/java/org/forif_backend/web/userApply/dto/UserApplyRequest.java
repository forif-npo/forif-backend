package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.forif_backend.application.user.dto.UserApplyCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@Schema(description = "스터디 수강 신청 요청")
public record UserApplyRequest(
    @Schema(description = "지원할 스터디 ID", example = "1")
    @NotNull
    Integer studyId,

    @Schema(description = "지원 동기 (정규스터디는 50자 이상 500자 이하, 자율부원은 미입력)", example = "이 스터디에 지원하는 이유는...", nullable = true)
    @Length(min = 50, max = 500, message = "지원 사유는 50자 이상 500자 이내로 작성해주세요.")
    String applyReason,

    @Schema(description = "지원 순위 (정규스터디: 1 = 1순위, 2 = 2순위 / 자율부원: 미입력)", example = "1", nullable = true)
    @Min(value = 1, message = "지원 순위는 1 또는 2만 가능합니다.")
    @Max(value = 2, message = "지원 순위는 1 또는 2만 가능합니다.")
    Integer priority
) {

    public UserApplyCommand toCommand() {
        return new UserApplyCommand(studyId, applyReason, priority);
    }
}
