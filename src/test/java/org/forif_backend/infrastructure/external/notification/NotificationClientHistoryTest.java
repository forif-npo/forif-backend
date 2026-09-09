package org.forif_backend.infrastructure.external.notification;

import com.solapi.sdk.message.dto.request.MessageListRequest;
import com.solapi.sdk.message.dto.response.MessageListResponse;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.model.kakao.KakaoOption;
import com.solapi.sdk.message.service.DefaultMessageService;
import org.forif_backend.application.notification.dto.NotificationHistoryPage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationClientHistoryTest {

    @Test
    void preservesSolapiHistoryOrderWithCursor() {
        DefaultMessageService messageService = mock(DefaultMessageService.class);
        NotificationClient notificationClient = new NotificationClient();
        ReflectionTestUtils.setField(notificationClient, "messageService", messageService);

        Message message = createMessage("message-1", "2026-09-08T10:00:00.000+09:00");
        Message newerMessage = createMessage("message-2", "2026-09-09T10:00:00.000+09:00");

        Map<String, Message> providerMessages = new LinkedHashMap<>();
        providerMessages.put("message-1", message);
        providerMessages.put("message-2", newerMessage);
        MessageListResponse providerResponse = new MessageListResponse(providerMessages);
        providerResponse.setNextKey("next-key");
        when(messageService.getMessageList(any(MessageListRequest.class))).thenReturn(providerResponse);
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        NotificationHistoryPage result = notificationClient.getAlimTalkHistory("current-key", 50);

        LocalDateTime after = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        ArgumentCaptor<MessageListRequest> requestCaptor = ArgumentCaptor.forClass(MessageListRequest.class);
        verify(messageService).getMessageList(requestCaptor.capture());
        MessageListRequest request = requestCaptor.getValue();
        assertThat(request.getType()).isEqualTo("ATA");
        assertThat(request.getStartKey()).isEqualTo("current-key");
        assertThat(request.getLimit()).isEqualTo(50);
        assertThat(request.getStartDate()).isBetween(before.minusMonths(6), after.minusMonths(6));
        assertThat(request.getEndDate()).isBetween(before, after);
        assertThat(result.content()).extracting(history -> history.messageId())
                .containsExactly("message-1", "message-2");
        assertThat(result.content().get(0)).satisfies(history -> {
            assertThat(history.messageId()).isEqualTo("message-1");
            assertThat(history.templateId()).isEqualTo("template-1");
            assertThat(history.receiver()).isEqualTo("01012345678");
            assertThat(history.status()).isEqualTo("SENT");
            assertThat(history.processedAt()).isEqualTo("2026-09-08T10:00:01.000+09:00");
        });
        assertThat(result.nextCursor()).isEqualTo("next-key");
        assertThat(result.hasNext()).isTrue();
    }

    private static Message createMessage(String messageId, String createdAt) {
        Message message = new Message();
        message.setMessageId(messageId);
        message.setTo("01012345678");
        message.setStatus("SENT");
        message.setStatusCode("2000");
        message.setDateCreated(createdAt);
        message.setDateProcessed("2026-09-08T10:00:01.000+09:00");
        message.setDateReported("2026-09-08T10:00:02.000+09:00");
        message.setDateUpdated("2026-09-08T10:00:02.000+09:00");
        KakaoOption kakaoOption = new KakaoOption();
        kakaoOption.setTemplateId("template-1");
        message.setKakaoOptions(kakaoOption);
        return message;
    }
}
