package org.forif_backend.web.notification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;
import org.forif_backend.application.notification.dto.TemplateInfo;
import org.forif_backend.application.notification.NotificationService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.notification.dto.NotificationDtoMapper;
import org.forif_backend.web.notification.dto.SendAlimTalkRequest;
import org.forif_backend.web.notification.dto.SendAlimTalkResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림톡 발송
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SendAlimTalkResponse>> sendAlimTalk(
            @RequestBody @Valid SendAlimTalkRequest request,
            @AuthenticationPrincipal Long userId) {

        log.info("알림톡 발송 요청 - userId: {}, receivers: {}, templateCode: {}",
                userId, request.receivers().size(), request.templateCode());

        SendAlimTalkCommand command = NotificationDtoMapper.toCommand(request);

        SendAlimTalkResult result = notificationService.sendAlimTalk(command, userId).join();

        SendAlimTalkResponse response = NotificationDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 발송 가능한 카카오 알림톡 템플릿 조회
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<TemplateInfo>>> getKakaoTemplates(
            @RequestParam Long userId) {

        log.info("알림톡 템플릿 조회 요청 - userId: {}", userId);

        List<TemplateInfo> templates = notificationService.getKakaoTemplates(userId);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }
}