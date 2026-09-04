package org.forif_backend.web.dues;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.dues.dto.DuesPageResult;
import org.forif_backend.application.dues.dto.UpdateDuesMemberCommand;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.web.dues.dto.BatchUpdateDuesRequest;
import org.forif_backend.web.dues.dto.RegistrationWithdrawalRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "회비 관리", description = "현재 학기 합격자의 회비·구글폼 제출 상태 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/dues")
public class DuesController {

    private final DuesService duesService;

    @Operation(summary = "현재 학기 회비 관리 목록 조회 (어드민 전용)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DuesPageResult>> getCurrentSemesterDues(
            @Parameter(description = "페이지 번호, 0부터 시작") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 당 항목 수, 최대 100") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "이름 또는 학과 검색어") @RequestParam(required = false) String search,
            @Parameter(description = "정렬 조건 (예: userName:asc)") @RequestParam(value = "sort", required = false) List<String> sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                duesService.getCurrentSemesterDues(page, size, search,
                        SortCriteria.parse(sort, Set.of("userId", "userName", "department", "googleFormSubmitted", "duesPaid")))
        ));
    }

    @Operation(summary = "현재 학기 회비·구글폼 상태 일괄 저장 (어드민 전용)")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateCurrentSemesterDuesBatch(
            @RequestBody @Valid BatchUpdateDuesRequest request
    ) {
        duesService.updateCurrentSemesterDuesBatch(
                request.updates().stream()
                        .map(item -> new UpdateDuesMemberCommand(
                                item.userId(),
                                item.duesPaid(),
                                item.googleFormSubmitted()
                        ))
                        .toList()
        );
        return ResponseEntity.ok(ApiResponse.successWithMsg("회비 관리 상태를 저장했습니다."));
    }

    @Operation(summary = "현재 학기 등록 철회 (어드민 전용)",
            description = "합격 및 신청 이력은 보존하고, 선택한 사용자를 회비 관리와 현재 학기 활동부원 등록 대상에서 제외합니다.")
    @PostMapping("/registration-withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> withdrawCurrentSemesterRegistrations(
            @RequestBody @Valid RegistrationWithdrawalRequest request
    ) {
        duesService.withdrawCurrentSemesterRegistrations(request.userIds());
        return ResponseEntity.ok(ApiResponse.successWithMsg("이번 학기 등록을 철회했습니다."));
    }
}
