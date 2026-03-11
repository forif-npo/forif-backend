package org.forif_backend.web.notification;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "알림", description = "카카오 알림톡 발송 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림톡 발송
     */
    @Operation(summary = "알림톡 발송 (어드민 전용)", description = "지정한 수신자들에게 카카오 알림톡을 발송합니다.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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
    @Operation(summary = "알림톡 템플릿 조회 (어드민 전용)", description = "발송 가능한 카카오 알림톡 템플릿 목록을 조회합니다.")
    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TemplateInfo>>> getKakaoTemplates(
            @AuthenticationPrincipal Long userId) {

        log.info("알림톡 템플릿 조회 요청 - userId: {}", userId);

        List<TemplateInfo> templates = notificationService.getKakaoTemplates(userId);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }
}
