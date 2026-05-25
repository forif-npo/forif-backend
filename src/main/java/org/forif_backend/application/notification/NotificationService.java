package org.forif_backend.application.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;
import org.forif_backend.application.notification.dto.TemplateInfo;
import org.forif_backend.application.notification.port.out.NotificationSendPort;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationSendPort notificationSendPort;
    private final StaffAccountRepository staffAccountRepository;
    private final UserRepository userRepository;

    public CompletableFuture<SendAlimTalkResult> sendAlimTalk(
            SendAlimTalkCommand command,
            Long senderId)  {

        // 권한 확인
        staffAccountRepository.findByUserId(senderId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        // 수신자별 이름 조회 (전화번호 -> 이름)
        Map<String, String> receiverNames = command.receivers().stream()
                .distinct()
                .collect(Collectors.toMap(
                        phoneNumber -> phoneNumber,
                        phoneNumber -> userRepository.findByPhoneNum(phoneNumber)
                                .map(User::getUserName)
                                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND))
                ));

        // 알림톡 전송 (수신자별 이름 포함)
        return notificationSendPort.sendAlimTalk(command, receiverNames);
    }

    public List<TemplateInfo> getKakaoTemplates(Long userId) {
        staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        return notificationSendPort.getKakaoTemplates();
    }
}
