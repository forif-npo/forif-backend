package org.forif_backend.web.dues.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RegistrationWithdrawalRequest(
        @NotEmpty(message = "등록을 철회할 부원이 없습니다.")
        @JsonProperty("user_ids")
        List<@NotNull(message = "부원 ID는 필수입니다.") Long> userIds
) {
}
