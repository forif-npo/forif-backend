package org.forif_backend.web.study;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.MentorConfirmationService;
import org.forif_backend.application.study.dto.MentorConfirmationStatusResult;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.study.dto.MentorConfirmationStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MentorConfirmationController {

    private final MentorConfirmationService mentorConfirmationService;

    @GetMapping("/api/v1/studies/{studyId}/mentor-confirmation")
    public ResponseEntity<ApiResponse<MentorConfirmationStatusResponse>> getMyMentorConfirmation(
            @PathVariable Integer studyId,
            @AuthenticationPrincipal Long userId
    ) {
        MentorConfirmationStatusResult result = mentorConfirmationService.getMyConfirmation(studyId, userId);
        return ResponseEntity.ok(ApiResponse.success(MentorConfirmationStatusResponse.from(result)));
    }
}
