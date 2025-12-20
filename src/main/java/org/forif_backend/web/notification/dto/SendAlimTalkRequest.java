package org.forif_backend.web.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SendAlimTalkRequest(
        @NotEmpty(message = "수신자 목록은 비어있을 수 없습니다.")
        @JsonProperty("receivers")
        List<String> receivers,

        @NotBlank(message = "템플릿 코드는 필수입니다.")
        @JsonProperty("templateCode")
        String templateCode,

        @JsonProperty("studyName")
        String studyName,

        @JsonProperty("responseSchedule")
        String responseSchedule,

        @JsonProperty("dateTime")
        String dateTime,

        @JsonProperty("location")
        String location,

        @JsonProperty("url")
        String url
) {
}