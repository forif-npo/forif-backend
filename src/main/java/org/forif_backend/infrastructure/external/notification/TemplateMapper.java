package org.forif_backend.infrastructure.external.notification;

import com.solapi.sdk.message.dto.response.kakao.KakaoAlimtalkTemplateResponse;
import org.forif_backend.application.notification.dto.TemplateInfo;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemplateMapper {

    public static TemplateInfo toTemplateInfo(KakaoAlimtalkTemplateResponse sdkResponse) {
        return new TemplateInfo(
                sdkResponse.getTemplateId(),
                sdkResponse.getName(),
                sdkResponse.getContent(),
                sdkResponse.getStatus() != null ? sdkResponse.getStatus().toString() : null,
                sdkResponse.getMessageType() != null ? sdkResponse.getMessageType().toString() : null,
                sdkResponse.getDateCreated(),
                sdkResponse.getDateUpdated(),
                sdkResponse.getVariables() == null
                        ? List.of()
                        : sdkResponse.getVariables().stream()
                                .map(KakaoAlimtalkTemplateResponse.KakaoAlimtalkTemplateVariable::getName)
                                .toList(),
                sdkResponse.getButtons() == null
                        ? List.of()
                        : sdkResponse.getButtons().stream()
                                .flatMap(button -> Stream.of(
                                        button.getLinkMo(),
                                        button.getLinkPc(),
                                        button.getLinkAnd(),
                                        button.getLinkIos()
                                ))
                                .filter(Objects::nonNull)
                                .toList()
        );
    }

    public static List<TemplateInfo> toTemplateInfoList(List<KakaoAlimtalkTemplateResponse> sdkResponses) {
        return sdkResponses.stream()
                .map(TemplateMapper::toTemplateInfo)
                .collect(Collectors.toList());
    }
}
