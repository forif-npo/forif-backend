package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.forif_backend.domain.user.UserApplyStatus;

@Schema(description = "신청서 상태 변경 요청")
public record UserApplyStatusUpdateRequest(
        @Schema(description = "변경할 상태 (PENDING=대기중, REJECT=탈락). 합격 처리는 /accept 엔드포인트를 사용해주세요.", example = "REJECT")
        @NotNull UserApplyStatus status
) {
}
