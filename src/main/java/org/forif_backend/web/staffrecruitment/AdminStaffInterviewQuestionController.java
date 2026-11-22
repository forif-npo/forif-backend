package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staffrecruitment.StaffInterviewQuestionService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewQuestionRequests.CreateRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewQuestionRequests.UpdateRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewQuestionResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 면접 질문 관리. 회장단이 공통 질문과 부서별 질문을 쓴다. */
@Tag(name = "운영진 모집 면접 질문 (회장단)", description = "공통 질문과 부서별 질문")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff-recruitments/{recruitmentId}/interview-questions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffInterviewQuestionController {

    private final StaffInterviewQuestionService staffInterviewQuestionService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "면접 질문 목록", description = "공통 질문이 먼저, 부서 질문은 부서 순서대로 나옵니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffInterviewQuestionResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(HttpStatus.OK, staffInterviewQuestionService.getQuestions(recruitmentId).stream()
                .map(StaffInterviewQuestionResponse::from).toList());
    }

    @Operation(summary = "면접 질문 추가", description = "department_id를 비우면 공통 질문입니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<StaffInterviewQuestionResponse>> create(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody CreateRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(HttpStatus.CREATED, StaffInterviewQuestionResponse.from(
                staffInterviewQuestionService.create(recruitmentId, request.departmentId(), request.content())));
    }

    @Operation(summary = "면접 질문 수정")
    @PatchMapping("/{questionId}")
    public ResponseEntity<ApiResponse<StaffInterviewQuestionResponse>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer questionId,
            @Valid @RequestBody UpdateRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(HttpStatus.OK, StaffInterviewQuestionResponse.from(
                staffInterviewQuestionService.updateContent(recruitmentId, questionId, request.content())));
    }

    @Operation(summary = "면접 질문 삭제")
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer questionId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        staffInterviewQuestionService.delete(recruitmentId, questionId);
        return ResponseEntity.noContent().build();
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(HttpStatus status, T body) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(body));
    }
}
