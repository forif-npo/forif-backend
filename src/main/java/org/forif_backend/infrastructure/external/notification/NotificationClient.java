package org.forif_backend.infrastructure.external.notification;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.dto.request.kakao.KakaoAlimtalkSendableTemplateListRequest;
import com.solapi.sdk.message.dto.response.kakao.KakaoAlimtalkTemplateResponse;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.FailedMessage;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.model.kakao.KakaoOption;
import com.solapi.sdk.message.service.DefaultMessageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkMessageResult;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;
import org.forif_backend.application.notification.dto.TemplateInfo;
import org.forif_backend.application.notification.port.out.NotificationSendPort;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient implements NotificationSendPort {

    @Value("${notification.api-key}")
    private String apiKey;

    @Value("${notification.api-secret}")
    private String apiSecret;

    @Value("${notification.pf-id}")
    private String pfId;

    @Value("${notification.sender-number}")
    private String senderNumber;

    private DefaultMessageService messageService;

    @PostConstruct
    public void init() {
        log.info("Initializing NotificationClient with environment variables");
        this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
        log.info("NotificationClient initialized successfully");
    }

    @Override
    public CompletableFuture<SendAlimTalkResult> sendAlimTalk(
            SendAlimTalkCommand command,
            Map<String, String> receiverNames) {

        return CompletableFuture.supplyAsync(() -> {
            // 수신자별로 메시지 생성
            List<Message> messages = command.receivers().stream()
                    .map(phoneNumber -> {
                        // 해당 전화번호의 이름 가져오기
                        String name = receiverNames.get(phoneNumber);

                        // 수신자별 변수 생성
                        HashMap<String, String> variables = getVariables(command, name);

                        // 카카오 옵션 설정
                        KakaoOption kakaoOption = new KakaoOption();
                        kakaoOption.setPfId(pfId);
                        kakaoOption.setTemplateId(command.templateCode());
                        kakaoOption.setDisableSms(true);
                        kakaoOption.setVariables(variables);

                        // 메시지 생성
                        Message message = new Message();
                        message.setFrom(senderNumber);
                        message.setTo(phoneNumber);
                        message.setKakaoOptions(kakaoOption);

                        return message;
                    })
                    .collect(Collectors.toList());

            try {
                // 일괄 발송
                messageService.send(messages);

                log.info("Bulk message sent successfully - templateId: {}, receivers: {}",
                        command.templateCode(), command.receivers().size());
                List<SendAlimTalkMessageResult> results = command.receivers().stream()
                        .map(r -> new SendAlimTalkMessageResult(r, true, null, null))
                        .collect(Collectors.toList());

                return new SendAlimTalkResult(command.templateCode(), results);

            } catch (SolapiMessageNotReceivedException exception) {
                log.error("Failed to send bulk message - templateId: {}", command.templateCode(), exception);

                Map<String, FailedMessage> failedMessages = exception.getFailedMessageList().stream()
                        .collect(Collectors.toMap(FailedMessage::getTo, failedMessage -> failedMessage, (first, second) -> first));

                List<SendAlimTalkMessageResult> results = command.receivers().stream()
                        .map(receiver -> {
                            FailedMessage failedMessage = failedMessages.get(receiver);
                            if (failedMessage == null) {
                                return new SendAlimTalkMessageResult(receiver, true, null, null);
                            }

                            return new SendAlimTalkMessageResult(
                                    receiver,
                                    false,
                                    failedMessage.getStatusCode(),
                                    failedMessage.getStatusMessage()
                            );
                        })
                        .collect(Collectors.toList());

                return new SendAlimTalkResult(command.templateCode(), results);

            } catch (Exception exception) {
                log.error("Unexpected error while sending bulk message", exception);
                throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @NotNull
    private static HashMap<String, String> getVariables(SendAlimTalkCommand command, String name) {
        HashMap<String, String> variables = command.variables() == null
                ? new HashMap<>()
                : new HashMap<>(command.variables());
        variables.put("#{이름}", name);  // 수신자별 이름
        return variables;
    }

    @Override
    public List<TemplateInfo> getKakaoTemplates() {
        try {
            KakaoAlimtalkSendableTemplateListRequest request = new KakaoAlimtalkSendableTemplateListRequest();
            request.setChannelId(pfId);

            List<KakaoAlimtalkTemplateResponse> sdkTemplates =
                    messageService.getSendableKakaoAlimtalkTemplates(request);

            log.info("Successfully retrieved {} kakao templates", sdkTemplates.size());

            return TemplateMapper.toTemplateInfoList(sdkTemplates);

        } catch (Exception e) {
            log.error("Failed to get kakao templates", e);
            throw new ForifException(ErrorCode.NOTIFICATION_TEMPLATE_FETCH_FAILED);
        }
    }
}
