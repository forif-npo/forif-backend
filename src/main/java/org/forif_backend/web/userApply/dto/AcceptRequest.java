package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "합격 처리 요청")
public record AcceptRequest(
        @Schema(description = "합격 처리할 지원자 ID 목록", example = "[1, 2, 3]")
        @NotEmpty
        List<Long> applierIds
) {
}
