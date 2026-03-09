package org.forif_backend.web.userApply;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.application.user.dto.ApplyDetailInfo;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.PageResponse;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.web.userApply.dto.UserApplyRequest;
import org.forif_backend.web.userApply.dto.UserApplyStatusUpdateRequest;
import org.forif_backend.web.userApply.dto.UserApplyDetailResponse;
import org.forif_backend.web.userApply.dto.UserApplyResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스터디 수강 신청", description = "멘티의 스터디 수강 신청 및 멘토의 신청서 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/apply")
public class UserApplyController {
    private final UserApplyService userApplyService;

    @Operation(summary = "스터디 수강 신청", description = "1지망 스터디와 선택적으로 2지망 스터디에 수강 신청합니다. 학기당 1회만 가능합니다.")
    @PostMapping("")
    public ResponseEntity<ApiResponse<Void>> applyStudy(@AuthenticationPrincipal Long userId,
                                                        @Valid @RequestBody UserApplyRequest userApplyRequest) {
        userApplyService.applyStudy(userId, userApplyRequest);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(summary = "신청자 목록 조회 (멘토 전용)", description = "해당 스터디에 신청한 멘티 목록을 조회합니다. 상태 필터 및 날짜 정렬을 지원합니다.")
    @GetMapping("/{studyId}")
    public ResponseEntity<ApiResponse<PageResponse<UserApplyResponse>>> getUserApply(
                                                                             @AuthenticationPrincipal Long userId,
                                                                             @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                             @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                                             @Parameter(description = "페이지 당 항목 수", example = "20") @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
                                                                             @Parameter(description = "상태 필터 (PENDING=대기중, ACCEPT=합격, WAITLIST=예비, REJECT=탈락). 미입력 시 전체 조회") @RequestParam(value = "statusFilter", required = false) UserApplyStatus userApplyStatus,
                                                                             @Parameter(description = "신청일 정렬 방향 (DESC=최신순, ASC=오래된순)", example = "DESC") @RequestParam(value = "applyDateDirection", required = false, defaultValue = "DESC") SortDirection sortDirection) {
        Page<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, page, pageSize, userApplyStatus, sortDirection);
        List<UserApplyResponse> responseContent = applyInfo.stream().map(UserApplyResponse::from).toList();
        PageResponse<UserApplyResponse> response = PageResponse.<UserApplyResponse>builder().totalPages(applyInfo.getTotalPages()).totalElements(applyInfo.getTotalElements()).content(responseContent).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "신청서 상세 조회 (멘토 전용)", description = "특정 신청서의 지원 동기 등 상세 내용을 조회합니다.")
    @GetMapping("/{studyId}/{applyId}")
    public ResponseEntity<ApiResponse<UserApplyDetailResponse>> getUserApplyDetail(
                                                                                   @AuthenticationPrincipal Long userId,
                                                                                   @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                                   @Parameter(description = "신청서 ID") @PathVariable("applyId") Long applyId) {
        ApplyDetailInfo applyDetailInfo = userApplyService.getApplyDetailInfo(userId, studyId, applyId);
        UserApplyDetailResponse response = UserApplyDetailResponse.from(applyDetailInfo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "신청서 상태 변경 (멘토 전용)", description = "신청서의 상태를 승인(ACCEPT), 예비(WAITLIST), 탈락(REJECT)으로 변경합니다. 예비 상태일 경우 반드시 waitlist_order를 함께 입력해야 합니다.")
    @PatchMapping("/{studyId}/{applyId}/status")
    public ResponseEntity<ApiResponse<Void>> updateApplyStatus(
                                                                @AuthenticationPrincipal Long userId,
                                                                @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                @Parameter(description = "신청서 ID") @PathVariable("applyId") Long applyId,
                                                                @Valid @RequestBody UserApplyStatusUpdateRequest request) {
        userApplyService.updateApplyStatus(userId, studyId, applyId, request);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(
            summary = "예비 승격 (멘토 전용)",
            description = """
                    합격자가 최종 신청을 포기한 경우 호출합니다.

                    **동작 순서**
                    1. 지정한 합격자(applyId)를 탈락(REJECT) 처리
                    2. 해당 스터디의 예비 1번을 합격(ACCEPT)으로 승격
                    3. 예비 2번 이후 순번을 1씩 당김

                    > 예비 대기자가 없는 경우 합격 취소만 처리됩니다.
                    """
    )
    @PostMapping("/{studyId}/{applyId}/promote")
    public ResponseEntity<ApiResponse<Void>> promoteWaitlist(
                                                              @AuthenticationPrincipal Long userId,
                                                              @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                              @Parameter(description = "최종 신청을 포기한 합격자의 신청서 ID") @PathVariable("applyId") Long applyId) {
        userApplyService.promoteWaitlist(userId, studyId, applyId);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(
            summary = "최종 등록 확정 (멘토 전용)",
            description = """
                    합격자가 최종 등록을 완료했을 때 호출합니다.

                    **동작 순서**
                    1. 해당 신청서가 합격(ACCEPT) 상태인지 검증
                    2. 동일 유저가 다른 스터디의 예비(WAITLIST) 대기열에 있는 경우 자동 제거
                    3. 제거된 예비 순번보다 뒤에 있는 대기자들의 순번을 1씩 당김

                    > 합격 상태가 아닌 신청서에 호출하면 400 오류가 반환됩니다.
                    """
    )
    @PostMapping("/{studyId}/{applyId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEnrollment(
                                                                @AuthenticationPrincipal Long userId,
                                                                @Parameter(description = "최종 등록할 스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                @Parameter(description = "최종 등록을 완료한 합격자의 신청서 ID") @PathVariable("applyId") Long applyId) {
        userApplyService.confirmEnrollment(userId, studyId, applyId);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }
}
