package org.forif_backend.infrastructure.external.notification;

import com.solapi.sdk.message.dto.response.kakao.KakaoAlimtalkTemplateResponse;
import org.forif_backend.application.notification.dto.TemplateInfo;

import java.util.List;
import java.util.stream.Collectors;

public class TemplateMapper {

    public static TemplateInfo toTemplateInfo(KakaoAlimtalkTemplateResponse sdkResponse) {
        return new TemplateInfo(
                sdkResponse.getTemplateId(),
                sdkResponse.getName(),
                sdkResponse.getContent(),
                sdkResponse.getStatus() != null ? sdkResponse.getStatus().toString() : null,
                sdkResponse.getMessageType() != null ? sdkResponse.getMessageType().toString() : null,
                sdkResponse.getDateCreated(),
                sdkResponse.getDateUpdated()
        );
    }

    public static List<TemplateInfo> toTemplateInfoList(List<KakaoAlimtalkTemplateResponse> sdkResponses) {
        return sdkResponses.stream()
                .map(TemplateMapper::toTemplateInfo)
                .collect(Collectors.toList());
    }
}