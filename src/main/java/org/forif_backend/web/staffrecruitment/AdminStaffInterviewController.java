package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staffrecruitment.StaffInterviewBoardService;
import org.forif_backend.application.staffrecruitment.StaffInterviewStaffRegistrationService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewRequests.*;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewResponses.BoardResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewResponses.SheetResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewResponses.StaffRegistrationResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 면접 관리. 보드에서 면접자 상태를 옮기고, 면접 중에 답변과 기타 질문을 적는다.
 * 지원서 내용은 기존 지원서 상세 API로 읽는다.
 */
@Tag(name = "운영진 모집 면접 관리 (회장단)", description = "면접 보드, 면접 기록지, 답변·기타 질문")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff-recruitments/{recruitmentId}/interviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffInterviewController {

    private final StaffInterviewBoardService staffInterviewBoardService;
    private final StaffInterviewStaffRegistrationService staffInterviewStaffRegistrationService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "면접 보드", description = "지원자 카드와 상태(면접 전·면접 완료·합격·불합격)입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<BoardResponse>> board(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(BoardResponse.from(staffInterviewBoardService.getBoard(recruitmentId)));
    }

    @Operation(summary = "면접 결과 최종 확정",
            description = "모든 지원자가 합격 또는 불합격일 때 확정합니다. 확정 후 결과는 변경할 수 없습니다.")
    @PostMapping("/finalize-results")
    public ResponseEntity<ApiResponse<BoardResponse>> finalizeResults(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(BoardResponse.from(staffInterviewBoardService.finalizeResults(recruitmentId)));
    }

    @Operation(summary = "면접 기록지", description = "공통·1지망·2지망 부서 질문과 답변, 기타 질문입니다.")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<SheetResponse>> sheet(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer applicationId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(SheetResponse.from(staffInterviewBoardService.getSheet(recruitmentId, applicationId)));
    }

    @Operation(summary = "면접 상태 변경", description = "보드에서 옮기거나 면접 완료·합격·불합격 버튼으로 바꿉니다.")
    @PutMapping("/{applicationId}/status")
    public ResponseEntity<ApiResponse<SheetResponse>> changeStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer applicationId,
            @Valid @RequestBody StatusRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(SheetResponse.from(
                staffInterviewBoardService.changeStatus(
                        recruitmentId, applicationId, request.status(), request.departmentId())));
    }

    @Operation(summary = "질문 답변 저장", description = "빈 답변을 보내면 지웁니다.")
    @PutMapping("/{applicationId}/answers/{questionId}")
    public ResponseEntity<ApiResponse<SheetResponse>> saveAnswer(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer applicationId,
            @PathVariable Integer questionId,
            @Valid @RequestBody AnswerRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(SheetResponse.from(staffInterviewBoardService.saveAnswer(
                recruitmentId, applicationId, questionId, request.answer())));
    }

    @Operation(summary = "기타 질문 추가")
    @PostMapping("/{applicationId}/extra-questions")
    public ResponseEntity<ApiResponse<SheetResponse>> addExtra(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer applicationId,
            @Valid @RequestBody ExtraQuestionCreateRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(SheetResponse.from(
                staffInterviewBoardService.addExtraQuestion(recruitmentId, applicationId, request.content())));
    }

    @Operation(summary = "기타 질문 수정", description = "보낸 필드만 바꿉니다.")
    @PatchMapping("/{applicationId}/extra-questions/{extraQuestionId}")
    public ResponseEntity<ApiResponse<SheetResponse>> updateExtra(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer applicationId,
            @PathVariable Integer extraQuestionId,
            @Valid @RequestBody ExtraQuestionUpdateRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(SheetResponse.from(staffInterviewBoardService.updateExtraQuestion(
                recruitmentId, applicationId, extraQuestionId, request.content(), request.answer())));
    }

    @Operation(summary = "기타 질문 삭제")
    @DeleteMapping("/{applicationId}/extra-questions/{extraQuestionId}")
    public ResponseEntity<ApiResponse<SheetResponse>> deleteExtra(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer applicationId,
            @PathVariable Integer extraQuestionId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(SheetResponse.from(staffInterviewBoardService.deleteExtraQuestion(
                recruitmentId, applicationId, extraQuestionId)));
    }

    @Operation(summary = "합격자 운영진 등록",
            description = "면접 결과 최종 확정 후 합격자를 모집 회차의 활동 학기 운영진 명단에 배정 부서로 올립니다. "
                    + "이미 명단에 있는 사람은 건너뛰므로 여러 번 눌러도 됩니다. "
                    + "운영진 추가와 같은 권한 규칙(SW팀은 현직 회장만 등)을 따릅니다.")
    @PostMapping("/staff-registration")
    public ResponseEntity<ApiResponse<StaffRegistrationResponse>> registerPassedApplicants(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(StaffRegistrationResponse.from(
                staffInterviewStaffRegistrationService.registerPassedApplicants(userId, recruitmentId)));
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(body));
    }
}
