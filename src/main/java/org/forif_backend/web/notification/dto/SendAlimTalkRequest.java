package org.forif_backend.web.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record SendAlimTalkRequest(
        @NotEmpty(message = "수신자 목록은 비어있을 수 없습니다.")
        @JsonProperty("receivers")
        List<String> receivers,

        @NotBlank(message = "템플릿 코드는 필수입니다.")
        @JsonProperty("templateCode")
        String templateCode,

        @JsonProperty("variables")
        Map<String, String> variables
) {
}
