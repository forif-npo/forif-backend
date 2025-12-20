package org.forif_backend.application.notification.port.out;

import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;
import org.forif_backend.application.notification.dto.TemplateInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NotificationSendPort {

    CompletableFuture<SendAlimTalkResult> sendAlimTalk(
            SendAlimTalkCommand command,
            Map<String, String> receiverNames  // phoneNumber -> userName
    );

    List<TemplateInfo> getKakaoTemplates();
}