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
import org.forif_backend.web.userApply.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스터디 수강 신청", description = "멘티의 스터디 수강 신청 및 멘토의 신청서 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/apply")
public class UserApplyController {
    private final UserApplyService userApplyService;

    @Operation(summary = "스터디 수강 신청", description = "1순위 또는 2순위 스터디에 건별로 수강 신청합니다.")
    @PostMapping("")
    public ResponseEntity<ApiResponse<Void>> applyStudy(@AuthenticationPrincipal Long userId,
                                                        @Valid @RequestBody UserApplyRequest userApplyRequest) {
        userApplyService.applyStudy(userId, userApplyRequest);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(summary = "지원 상태 조회", description = "현재 유저의 이번 학기 지원 상태를 조회합니다. 프론트에서 지원서 작성 페이지 진입 가능 여부를 판단하는 데 사용합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ApplyStatusResponse>> getApplyStatus(@AuthenticationPrincipal Long userId) {
        ApplyStatusResponse response = userApplyService.getApplyStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "수강 신청서 수정", description = "본인의 수강 신청서를 수정합니다. PENDING 상태인 경우에만 스터디 변경 및 지원 동기 수정이 가능합니다.")
    @PatchMapping("/{applyId}")
    public ResponseEntity<ApiResponse<Void>> updateApplication(@AuthenticationPrincipal Long userId,
                                                               @Parameter(description = "신청서 ID") @PathVariable("applyId") Long applyId,
                                                               @Valid @RequestBody UserApplyUpdateRequest request) {
        userApplyService.updateApplication(userId, applyId, request);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(summary = "신청자 목록 조회 (멘토 전용)", description = "해당 스터디에 신청한 멘티 목록을 조회합니다. 상태 필터 및 날짜 정렬을 지원합니다.")
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/{studyId}")
    public ResponseEntity<ApiResponse<PageResponse<UserApplyResponse>>> getUserApply(
                                                                             @AuthenticationPrincipal Long userId,
                                                                             @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                             @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                                             @Parameter(description = "페이지 당 항목 수", example = "20") @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
                                                                             @Parameter(description = "상태 필터 (PENDING=대기중, ACCEPT=합격, REJECT=탈락). 미입력 시 전체 조회") @RequestParam(value = "statusFilter", required = false) UserApplyStatus userApplyStatus,
                                                                             @Parameter(description = "신청일 정렬 방향 (DESC=최신순, ASC=오래된순)", example = "DESC") @RequestParam(value = "applyDateDirection", required = false, defaultValue = "DESC") SortDirection sortDirection) {
        Page<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, page, pageSize, userApplyStatus, sortDirection);
        List<UserApplyResponse> responseContent = applyInfo.stream().map(UserApplyResponse::from).toList();
        PageResponse<UserApplyResponse> response = PageResponse.<UserApplyResponse>builder().totalPages(applyInfo.getTotalPages()).totalElements(applyInfo.getTotalElements()).content(responseContent).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "신청서 상세 조회 (멘토 전용)", description = "특정 신청서의 지원 동기 등 상세 내용을 조회합니다.")
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/{studyId}/{applyId}")
    public ResponseEntity<ApiResponse<UserApplyDetailResponse>> getUserApplyDetail(
                                                                                   @AuthenticationPrincipal Long userId,
                                                                                   @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                                   @Parameter(description = "신청서 ID") @PathVariable("applyId") Long applyId) {
        ApplyDetailInfo applyDetailInfo = userApplyService.getApplyDetailInfo(userId, studyId, applyId);
        UserApplyDetailResponse response = UserApplyDetailResponse.from(applyDetailInfo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "신청서 상태 변경 (멘토 전용)", description = "신청서의 상태를 PENDING(대기중) 또는 REJECT(탈락)으로 변경합니다. 합격 처리는 /accept 엔드포인트를 사용해주세요.")
    @PreAuthorize("hasRole('MENTOR')")
    @PatchMapping("/{studyId}/{applyId}/status")
    public ResponseEntity<ApiResponse<Void>> updateApplyStatus(
                                                                @AuthenticationPrincipal Long userId,
                                                                @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                @Parameter(description = "신청서 ID") @PathVariable("applyId") Long applyId,
                                                                @Valid @RequestBody UserApplyStatusUpdateRequest request) {
        userApplyService.updateApplyStatus(userId, studyId, applyId, request);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(summary = "합격 처리 (멘토 전용)", description = "신청서 ID 목록을 받아 일괄 합격 처리합니다. 이미 합격인 신청서는 스킵되고, 2순위 합격 상태에서 1순위 합격 시 2순위 StudyUser가 삭제됩니다.")
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/{studyId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptApplications(
                                                                 @AuthenticationPrincipal Long userId,
                                                                 @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                 @Valid @RequestBody AcceptRequest request) {
        userApplyService.acceptApplications(userId, studyId, request.applyIds());
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(summary = "불합격 처리 (멘토 전용)", description = "신청서 ID 목록을 받아 일괄 불합격 처리합니다. 이미 불합격인 신청서는 스킵되고, 합격 상태였다면 StudyUser가 삭제됩니다.")
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/{studyId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectApplications(
                                                                 @AuthenticationPrincipal Long userId,
                                                                 @Parameter(description = "스터디 ID") @PathVariable("studyId") Integer studyId,
                                                                 @Valid @RequestBody RejectRequest request) {
        userApplyService.rejectApplications(userId, studyId, request.applyIds());
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }
}
