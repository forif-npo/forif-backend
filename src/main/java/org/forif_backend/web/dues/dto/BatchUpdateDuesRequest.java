package org.forif_backend.web.dues.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchUpdateDuesRequest(
        @NotEmpty(message = "저장할 회비 관리 상태가 없습니다.")
        @JsonProperty("updates")
        List<@Valid Item> updates
) {
    public record Item(
            @NotNull(message = "부원 ID는 필수입니다.")
            @JsonProperty("userId")
            Long userId,

            @JsonProperty("duesPaid")
            Boolean duesPaid,

            @JsonProperty("googleFormSubmitted")
            Boolean googleFormSubmitted
    ) {
        @AssertTrue(message = "회비 납부 또는 구글폼 제출 상태 중 하나는 필요합니다.")
        public boolean isStatusProvided() {
            return duesPaid != null || googleFormSubmitted != null;
        }
    }
}
