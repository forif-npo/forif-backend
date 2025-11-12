package org.forif_backend.web.user;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.application.user.UserService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.user.dto.StudyApplyRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplyService userApplyService;

    @PostMapping("/study")
    public ResponseEntity<ApiResponse<Void>> applyStudy(@AuthenticationPrincipal Long userId,
                                                       @Valid @RequestBody StudyApplyRequest studyApplyRequest) {
        userApplyService.applyStudy(userId, studyApplyRequest);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }
}
