package org.forif_backend.web.dues;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.dues.dto.DuesMember;
import org.forif_backend.application.dues.dto.DuesPageResult;
import org.forif_backend.application.dues.dto.DuesSort;
import org.forif_backend.application.dues.dto.UpdateDuesMemberCommand;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.dues.dto.BatchUpdateDuesRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회비 관리", description = "현재 학기 부원의 회비·구글폼 제출 상태 관리 API")
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
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "NEEDS_ATTENTION") DuesSort sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                duesService.getCurrentSemesterDues(page, size, search, sort)
        ));
    }

    @Operation(summary = "현재 학기 회비·구글폼 상태 일괄 저장 (어드민 전용)")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<DuesMember>>> updateCurrentSemesterDuesBatch(
            @RequestBody @Valid BatchUpdateDuesRequest request
    ) {
        java.util.List<DuesMember> response = duesService.updateCurrentSemesterDuesBatch(
                request.updates().stream()
                        .map(item -> new UpdateDuesMemberCommand(
                                item.userId(),
                                item.duesPaid(),
                                item.googleFormSubmitted()
                        ))
                        .toList()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
