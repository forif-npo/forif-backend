package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.StaffApplicationService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationRequests.SubmitRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationResponse.DetailResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationResponse.SubmitResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 지원자 본인의 운영진 지원 API.
 * 로그인한 회원만 호출할 수 있고, 지원자 식별은 인증 컨텍스트에서만 얻는다.
 */
@Tag(name = "운영진 지원 (지원자)", description = "지원서 제출과 본인 지원 내역 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff-recruitments/{recruitmentId}/applications")
public class StaffApplicationController {

    private final StaffApplicationService staffApplicationService;

    @Operation(summary = "지원서 제출",
            description = "이번 학기 활동 부원만 제출할 수 있습니다. 한 회차당 한 번만 제출됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<SubmitResponse>> submit(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "모집 회차 ID") @PathVariable Integer recruitmentId,
            @Valid @RequestBody SubmitRequest request
    ) {
        SubmitResponse response = SubmitResponse.from(
                staffApplicationService.submit(recruitmentId, userId, request.toCommand()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "본인 지원서 조회",
            description = "본인이 제출한 지원서만 반환합니다. 제출하지 않았다면 404입니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DetailResponse>> getMine(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        DetailResponse response = DetailResponse.from(staffApplicationService.getMine(recruitmentId, userId));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }
}
