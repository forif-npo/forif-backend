package org.forif_backend.web.dues.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;

public record UpdateDuesRequest(
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
