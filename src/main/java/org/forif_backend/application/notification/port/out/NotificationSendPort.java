package org.forif_backend.application.notification.port.out;

import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;
import org.forif_backend.application.notification.dto.TemplateInfo;
import org.forif_backend.application.notification.dto.NotificationHistoryPage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NotificationSendPort {

    CompletableFuture<SendAlimTalkResult> sendAlimTalk(
            SendAlimTalkCommand command,
            Map<String, String> receiverNames  // phoneNumber -> userName
    );

    List<TemplateInfo> getKakaoTemplates();

    /**
     * Solapi에 보관된 알림톡(ATA) 발송 이력을 조회한다.
     * cursor는 Solapi가 반환한 nextKey를 그대로 사용한다.
     */
    NotificationHistoryPage getAlimTalkHistory(String cursor, int size);
}
