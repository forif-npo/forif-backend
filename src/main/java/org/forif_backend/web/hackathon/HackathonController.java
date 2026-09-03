package org.forif_backend.web.hackathon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.hackathon.HackathonService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.hackathon.HackathonStatus;
import org.forif_backend.domain.hackathon.JoinRequestStatus;
import org.forif_backend.domain.hackathon.ParticipantStatus;
import org.forif_backend.web.hackathon.dto.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "해커톤", description = "해커톤 운영 API")
@RestController
@RequiredArgsConstructor
public class HackathonController {

    private final HackathonService hackathonService;

    @Operation(summary = "해커톤 생성 (어드민 전용)")
    @PostMapping("/api/v1/admin/hackathons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HackathonIdResponse>> createHackathon(
            @Valid @RequestBody CreateHackathonRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.createHackathon(request)));
    }

    @Operation(summary = "해커톤 목록 조회")
    @GetMapping("/api/v1/hackathons")
    public ResponseEntity<ApiResponse<CursorPageResponse<HackathonResponse>>> getHackathons(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) HackathonStatus status,
            @Parameter(description = "해커톤 제목, 장소, 학기 또는 기수 검색어") @RequestParam(required = false) String search,
            @Parameter(description = "이전 페이지의 마지막 해커톤 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.getHackathons(year, semester, status, search, cursor, page, size)));
    }

    @Operation(summary = "해커톤 상세 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}")
    public ResponseEntity<ApiResponse<HackathonDetailResponse>> getHackathon(
            @Parameter(description = "해커톤 ID") @PathVariable Long hackathonId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getHackathon(hackathonId)));
    }

    @Operation(summary = "해커톤 수정 (어드민 전용)")
    @PatchMapping("/api/v1/admin/hackathons/{hackathonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HackathonDetailResponse>> updateHackathon(
            @PathVariable Long hackathonId,
            @RequestBody UpdateHackathonRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.updateHackathon(hackathonId, request)));
    }

    @Operation(summary = "해커톤 상태 변경 (어드민 전용)")
    @PatchMapping("/api/v1/admin/hackathons/{hackathonId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changeHackathonStatus(
            @PathVariable Long hackathonId,
            @Valid @RequestBody UpdateHackathonStatusRequest request
    ) {
        hackathonService.changeHackathonStatus(hackathonId, request.status());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "해커톤 삭제 (어드민 전용)")
    @DeleteMapping("/api/v1/admin/hackathons/{hackathonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteHackathon(@PathVariable Long hackathonId) {
        hackathonService.deleteHackathon(hackathonId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "내 해커톤 참가 등록")
    @PostMapping("/api/v1/hackathons/{hackathonId}/participants/me")
    public ResponseEntity<ApiResponse<ParticipantResponse>> registerParticipant(
            @PathVariable Long hackathonId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.registerParticipant(hackathonId, userId)));
    }

    @Operation(summary = "내 해커톤 참가 취소")
    @DeleteMapping("/api/v1/hackathons/{hackathonId}/participants/me")
    public ResponseEntity<ApiResponse<Void>> cancelParticipant(
            @PathVariable Long hackathonId,
            @AuthenticationPrincipal Long userId
    ) {
        hackathonService.cancelParticipant(hackathonId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "내 해커톤 참가 상태 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}/participants/me")
    public ResponseEntity<ApiResponse<ParticipantResponse>> getMyParticipant(
            @PathVariable Long hackathonId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getMyParticipant(hackathonId, userId)));
    }

    @Operation(summary = "해커톤 참가자 목록 조회 (어드민 전용)")
    @GetMapping("/api/v1/admin/hackathons/{hackathonId}/participants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<ParticipantResponse>>> getParticipants(
            @PathVariable Long hackathonId,
            @RequestParam(required = false) ParticipantStatus status,
            @RequestParam(defaultValue = "false") boolean withoutTeam,
            @Parameter(description = "이전 페이지의 마지막 참가자 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.getParticipants(hackathonId, status, withoutTeam, cursor, page, size)));
    }

    @Operation(summary = "해커톤 팀 생성")
    @PostMapping("/api/v1/hackathons/{hackathonId}/teams")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @PathVariable Long hackathonId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.createTeam(hackathonId, userId, request)));
    }

    @Operation(summary = "해커톤 팀 목록 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}/teams")
    public ResponseEntity<ApiResponse<CursorPageResponse<TeamResponse>>> getTeams(
            @PathVariable Long hackathonId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "이전 페이지의 마지막 팀 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.getTeamsForParticipant(hackathonId, userId, cursor, page, size)));
    }

    @Operation(summary = "내 해커톤 팀 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}/teams/me")
    public ResponseEntity<ApiResponse<TeamResponse>> getMyTeam(
            @PathVariable Long hackathonId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getMyTeam(hackathonId, userId)));
    }

    @Operation(summary = "해커톤 팀 수정")
    @PatchMapping("/api/v1/hackathons/{hackathonId}/teams/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @RequestBody UpdateTeamRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.updateTeam(hackathonId, teamId, userId, request)));
    }

    @Operation(summary = "해커톤 팀 해산")
    @DeleteMapping("/api/v1/hackathons/{hackathonId}/teams/{teamId}")
    public ResponseEntity<ApiResponse<Void>> disbandTeam(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId
    ) {
        hackathonService.disbandTeam(hackathonId, teamId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "해커톤 팀/팀원 현황 조회 (어드민 전용)")
    @GetMapping("/api/v1/admin/hackathons/{hackathonId}/teams")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<TeamResponse>>> getAdminTeams(
            @PathVariable Long hackathonId,
            @Parameter(description = "이전 페이지의 마지막 팀 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getTeams(hackathonId, cursor, page, size)));
    }

    @Operation(summary = "해커톤 팀 삭제 (어드민 전용)")
    @DeleteMapping("/api/v1/admin/hackathons/{hackathonId}/teams/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAdminTeam(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId
    ) {
        hackathonService.deleteTeamByAdmin(hackathonId, teamId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "팀 가입 신청")
    @PostMapping("/api/v1/hackathons/{hackathonId}/teams/{teamId}/join-requests")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> createJoinRequest(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @RequestBody CreateJoinRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.createJoinRequest(hackathonId, teamId, userId, request)));
    }

    @Operation(summary = "팀 가입 신청 목록 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}/teams/{teamId}/join-requests")
    public ResponseEntity<ApiResponse<CursorPageResponse<JoinRequestResponse>>> getJoinRequests(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) JoinRequestStatus status,
            @Parameter(description = "이전 페이지의 마지막 가입 신청 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.getJoinRequests(hackathonId, teamId, userId, status, cursor, page, size)));
    }

    @Operation(summary = "팀 가입 승인")
    @PatchMapping("/api/v1/hackathons/{hackathonId}/join-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> approveJoinRequest(
            @PathVariable Long hackathonId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.approveJoinRequest(hackathonId, requestId, userId)));
    }

    @Operation(summary = "팀 가입 거절")
    @PatchMapping("/api/v1/hackathons/{hackathonId}/join-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> rejectJoinRequest(
            @PathVariable Long hackathonId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.rejectJoinRequest(hackathonId, requestId, userId)));
    }

    @Operation(summary = "결과물 제출")
    @PostMapping(value = "/api/v1/hackathons/{hackathonId}/teams/{teamId}/submission", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SubmissionResponse>> createSubmission(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @RequestPart("request") @Valid SubmissionRequest request,
            @RequestPart(value = "presentation", required = false) MultipartFile presentation
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.createSubmission(hackathonId, teamId, userId, request, presentation)));
    }

    @Operation(summary = "결과물 수정")
    @PutMapping(value = "/api/v1/hackathons/{hackathonId}/teams/{teamId}/submission", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SubmissionResponse>> updateSubmission(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @RequestPart("request") @Valid SubmissionRequest request,
            @RequestPart(value = "presentation", required = false) MultipartFile presentation
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.updateSubmission(hackathonId, teamId, userId, request, presentation)));
    }

    @Operation(summary = "제출물 목록 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}/submissions")
    public ResponseEntity<ApiResponse<CursorPageResponse<SubmissionResponse>>> getSubmissions(
            @PathVariable Long hackathonId,
            @Parameter(description = "이전 페이지의 마지막 제출물 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getSubmissions(hackathonId, cursor, page, size)));
    }

    @Operation(summary = "제출 현황 조회 (어드민 전용)")
    @GetMapping("/api/v1/admin/hackathons/{hackathonId}/submissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<SubmissionStatusResponse>>> getAdminSubmissions(
            @PathVariable Long hackathonId,
            @Parameter(description = "이전 페이지의 마지막 팀 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.getSubmissionStatuses(hackathonId, cursor, page, size)));
    }

    @Operation(summary = "평가 기준 생성 (어드민 전용)")
    @PostMapping("/api/v1/admin/hackathons/{hackathonId}/criteria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CriterionResponse>> createCriterion(
            @PathVariable Long hackathonId,
            @Valid @RequestBody CriterionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.createCriterion(hackathonId, request)));
    }

    @Operation(summary = "평가 기준 수정 (어드민 전용)")
    @PutMapping("/api/v1/admin/hackathons/{hackathonId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CriterionResponse>> updateCriterion(
            @PathVariable Long hackathonId,
            @PathVariable Long criterionId,
            @Valid @RequestBody CriterionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.updateCriterion(hackathonId, criterionId, request)));
    }

    @Operation(summary = "평가 기준 삭제 (어드민 전용)")
    @DeleteMapping("/api/v1/admin/hackathons/{hackathonId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCriterion(
            @PathVariable Long hackathonId,
            @PathVariable Long criterionId
    ) {
        hackathonService.deleteCriterion(hackathonId, criterionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "평가 기준 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}/criteria")
    public ResponseEntity<ApiResponse<CursorPageResponse<CriterionResponse>>> getCriteria(
            @PathVariable Long hackathonId,
            @Parameter(description = "이전 페이지의 마지막 평가 기준 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getCriteria(hackathonId, cursor, page, size)));
    }

    @Operation(summary = "팀 평가 제출")
    @PostMapping("/api/v1/hackathons/{hackathonId}/teams/{teamId}/evaluations")
    public ResponseEntity<ApiResponse<EvaluationResponse>> createEvaluation(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.createEvaluation(hackathonId, teamId, userId, request)));
    }

    @Operation(summary = "내 평가 수정")
    @PutMapping("/api/v1/hackathons/{hackathonId}/teams/{teamId}/evaluations/me")
    public ResponseEntity<ApiResponse<EvaluationResponse>> updateMyEvaluation(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.updateMyEvaluation(hackathonId, teamId, userId, request)));
    }

    @Operation(summary = "내 평가 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}/teams/{teamId}/evaluations/me")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getMyEvaluation(
            @PathVariable Long hackathonId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getMyEvaluation(hackathonId, teamId, userId)));
    }

    @Operation(summary = "평가 집계 결과 조회 (어드민 전용)")
    @GetMapping("/api/v1/admin/hackathons/{hackathonId}/evaluations/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<EvaluationSummaryResponse>>> getEvaluationSummary(
            @PathVariable Long hackathonId,
            @Parameter(description = "이전 페이지의 마지막 팀 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.getEvaluationSummary(hackathonId, cursor, page, size)));
    }

    @Operation(summary = "수상 결과 등록 (어드민 전용)")
    @PostMapping("/api/v1/admin/hackathons/{hackathonId}/awards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AwardResponse>> createAward(
            @PathVariable Long hackathonId,
            @Valid @RequestBody AwardRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.createAward(hackathonId, request)));
    }

    @Operation(summary = "수상 결과 수정 (어드민 전용)")
    @PutMapping("/api/v1/admin/hackathons/{hackathonId}/awards/{awardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AwardResponse>> updateAward(
            @PathVariable Long hackathonId,
            @PathVariable Long awardId,
            @Valid @RequestBody AwardRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.updateAward(hackathonId, awardId, request)));
    }

    @Operation(summary = "수상 결과 삭제 (어드민 전용)")
    @DeleteMapping("/api/v1/admin/hackathons/{hackathonId}/awards/{awardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAward(
            @PathVariable Long hackathonId,
            @PathVariable Long awardId
    ) {
        hackathonService.deleteAward(hackathonId, awardId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "수상 결과 조회")
    @GetMapping("/api/v1/hackathons/{hackathonId}/awards")
    public ResponseEntity<ApiResponse<CursorPageResponse<AwardResponse>>> getAwards(
            @PathVariable Long hackathonId,
            @Parameter(description = "이전 페이지의 마지막 수상 결과 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getAwards(hackathonId, cursor, page, size)));
    }

    @Operation(summary = "종료된 해커톤 목록 조회")
    @GetMapping("/api/v1/archive/hackathons")
    public ResponseEntity<ApiResponse<CursorPageResponse<HackathonResponse>>> getArchiveHackathons(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            @Parameter(description = "이전 페이지의 마지막 해커톤 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.getArchiveHackathons(year, semester, cursor, page, size)));
    }

    @Operation(summary = "해커톤 아카이브 상세 조회")
    @GetMapping("/api/v1/archive/hackathons/{hackathonId}")
    public ResponseEntity<ApiResponse<ArchiveHackathonDetailResponse>> getArchiveHackathon(@PathVariable Long hackathonId) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getArchiveHackathon(hackathonId)));
    }

    @Operation(summary = "아카이브 결과물 갤러리 조회")
    @GetMapping("/api/v1/archive/hackathons/{hackathonId}/submissions")
    public ResponseEntity<ApiResponse<CursorPageResponse<SubmissionResponse>>> getArchiveSubmissions(
            @PathVariable Long hackathonId,
            @Parameter(description = "프로젝트명, 한 줄 소개 또는 팀명 검색어") @RequestParam(required = false) String search,
            @RequestParam(name = "tech_stack", required = false) String techStack,
            @Parameter(description = "이전 페이지의 마지막 제출물 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hackathonService.getArchiveSubmissions(hackathonId, search, techStack, cursor, page, size)));
    }

    @Operation(summary = "아카이브 결과물 상세 조회")
    @GetMapping("/api/v1/archive/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<ArchiveSubmissionDetailResponse>> getArchiveSubmission(
            @PathVariable Long submissionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hackathonService.getArchiveSubmission(submissionId)));
    }

}
