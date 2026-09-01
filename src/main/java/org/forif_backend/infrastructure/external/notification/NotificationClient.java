package org.forif_backend.infrastructure.external.notification;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.dto.request.kakao.KakaoAlimtalkSendableTemplateListRequest;
import com.solapi.sdk.message.dto.response.MultipleDetailMessageSentResponse;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient implements NotificationSendPort {

    private static final String RECEIVER_CUSTOM_FIELD = "forifReceiver";
    private static final String UNKNOWN_FAILURE_CODE = "UNKNOWN";
    private static final String UNMATCHED_FAILURE_MESSAGE = "Solapi 실패 수신자를 식별하지 못했습니다.";
    private static final String GENERIC_FAILURE_MESSAGE = "Solapi에서 발송을 거절했습니다.";

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
                        message.setCustomFields(Map.of(RECEIVER_CUSTOM_FIELD, phoneNumber));
                        message.setKakaoOptions(kakaoOption);

                        return message;
                    })
                    .collect(Collectors.toList());

            try {
                // 일괄 발송
                MultipleDetailMessageSentResponse response = messageService.send(messages);

                List<SendAlimTalkMessageResult> results = buildSendResults(
                        command.receivers(),
                        response.getFailedMessageList(),
                        false
                );
                long failureCount = results.stream().filter(result -> !result.success()).count();
                log.info("Bulk message send completed - templateId: {}, receivers: {}, failures: {}",
                        command.templateCode(), command.receivers().size(), failureCount);

                return new SendAlimTalkResult(command.templateCode(), results);

            } catch (SolapiMessageNotReceivedException exception) {
                log.error("Failed to send bulk message - templateId: {}", command.templateCode(), exception);

                List<SendAlimTalkMessageResult> results = buildSendResults(
                        command.receivers(),
                        exception.getFailedMessageList(),
                        true
                );

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

    static List<SendAlimTalkMessageResult> buildSendResults(
            List<String> receivers,
            List<FailedMessage> failedMessages,
            boolean allFailed
    ) {
        List<FailedMessage> safeFailedMessages = failedMessages == null ? List.of() : failedMessages;
        Set<String> receiverKeys = receivers.stream()
                .map(NotificationClient::normalizePhoneNumber)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, FailedMessage> failuresByReceiver = new HashMap<>();
        boolean hasUnmatchedFailure = false;

        for (FailedMessage failedMessage : safeFailedMessages) {
            if (failedMessage == null) {
                hasUnmatchedFailure = true;
                continue;
            }
            String receiverKey = normalizePhoneNumber(resolveReceiver(failedMessage));
            if (receiverKey == null || !receiverKeys.contains(receiverKey)) {
                hasUnmatchedFailure = true;
                continue;
            }
            failuresByReceiver.putIfAbsent(receiverKey, failedMessage);
        }
        boolean hasUnknownReceiverFailure = hasUnmatchedFailure;

        return receivers.stream()
                .map(receiver -> {
                    FailedMessage failedMessage = failuresByReceiver.get(normalizePhoneNumber(receiver));
                    if (failedMessage != null) {
                        return failureResult(receiver, failedMessage);
                    }
                    if (allFailed) {
                        return unknownFailureResult(receiver, GENERIC_FAILURE_MESSAGE);
                    }
                    if (hasUnknownReceiverFailure) {
                        return unknownFailureResult(receiver, UNMATCHED_FAILURE_MESSAGE);
                    }
                    return new SendAlimTalkMessageResult(receiver, true, null, null);
                })
                .toList();
    }

    private static SendAlimTalkMessageResult failureResult(String receiver, FailedMessage failedMessage) {
        return new SendAlimTalkMessageResult(
                receiver,
                false,
                failedMessage.getStatusCode() == null ? UNKNOWN_FAILURE_CODE : failedMessage.getStatusCode(),
                failedMessage.getStatusMessage() == null ? GENERIC_FAILURE_MESSAGE : failedMessage.getStatusMessage()
        );
    }

    private static SendAlimTalkMessageResult unknownFailureResult(String receiver, String errorMessage) {
        return new SendAlimTalkMessageResult(receiver, false, UNKNOWN_FAILURE_CODE, errorMessage);
    }

    private static String resolveReceiver(FailedMessage failedMessage) {
        Map<String, String> customFields = failedMessage.getCustomFields();
        if (customFields != null && customFields.get(RECEIVER_CUSTOM_FIELD) != null) {
            return customFields.get(RECEIVER_CUSTOM_FIELD);
        }
        return failedMessage.getTo();
    }

    private static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.startsWith("0082")) {
            return "0" + digits.substring(4);
        }
        if (digits.startsWith("82")) {
            return "0" + digits.substring(2);
        }
        return digits;
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
