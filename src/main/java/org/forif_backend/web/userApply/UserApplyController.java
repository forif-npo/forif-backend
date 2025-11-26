package org.forif_backend.web.userApply;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.application.user.dto.ApplyDetailInfo;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.web.userApply.dto.UserApplyRequest;
import org.forif_backend.web.userApply.dto.UserApplyDetailResponse;
import org.forif_backend.web.userApply.dto.UserApplyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-apply")
public class UserApplyController {
    private final UserApplyService userApplyService;

    @PostMapping("")
    public ResponseEntity<ApiResponse<Void>> applyStudy(@AuthenticationPrincipal Long userId,
                                                        @Valid @RequestBody UserApplyRequest userApplyRequest) {
        userApplyService.applyStudy(userId, userApplyRequest);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @GetMapping("/{studyId}")
    public ResponseEntity<ApiResponse<List<UserApplyResponse>>> getUserApply(@AuthenticationPrincipal Long userId,
                                                                             @PathVariable("studyId") Integer studyId,
                                                                             @RequestParam(value = "page", required = false, defaultValue = "0") Long page,
                                                                             @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize,
                                                                             @RequestParam(value = "statusFilter", required = false) UserApplyStatus userApplyStatus,
                                                                             @RequestParam(value = "applyDateDirection", required = false, defaultValue = "DESC") SortDirection sortDirection) {
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, page, pageSize, userApplyStatus, sortDirection);
        List<UserApplyResponse> response = applyInfo.stream().map(UserApplyResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{studyId}/{applyId}")
    public ResponseEntity<ApiResponse<UserApplyDetailResponse>> getUserApplyDetail(@AuthenticationPrincipal Long userId,
                                                                                   @PathVariable("studyId") Integer studyId, @PathVariable("applyId") Long applyId) {
        ApplyDetailInfo applyDetailInfo = userApplyService.getApplyDetailInfo(userId, studyId, applyId);
        UserApplyDetailResponse response = UserApplyDetailResponse.from(applyDetailInfo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
