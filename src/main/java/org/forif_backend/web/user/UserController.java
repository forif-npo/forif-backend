package org.forif_backend.web.user;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.application.user.UserService;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.web.user.dto.StudyApplyRequest;
import org.forif_backend.web.user.dto.UserApplyResponse;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplyService userApplyService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Void>> applyStudy(@AuthenticationPrincipal Long userId,
                                                       @Valid @RequestBody StudyApplyRequest studyApplyRequest) {
        userApplyService.applyStudy(userId, studyApplyRequest);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @GetMapping("/apply/{studyId}")
    public ResponseEntity<ApiResponse<List<UserApplyResponse>>> getUserApply(@AuthenticationPrincipal Long userId,
                                                                     @PathVariable("studyId") Integer studyId,
                                                                     @RequestParam(value = "page", required = false, defaultValue = "0") Long page,
                                                                     @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize,
                                                                     @RequestParam(value = "statusFilter", required = false) UserApplyStatus userApplyStatus,
                                                                     @RequestParam(value = "applyDateDirection", required = false, defaultValue = "DESC") SortDirection sortDirection) {
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, page, pageSize, userApplyStatus, sortDirection);
        List<UserApplyResponse> response = applyInfo.stream().map(UserApplyResponse::toUserApplyResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
