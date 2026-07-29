package org.forif_backend.web.semester;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.SemesterTransitionService;
import org.forif_backend.application.semester.dto.SemesterChangePreview;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.semester.dto.SemesterChangePreviewResponse;
import org.forif_backend.web.semester.dto.SemesterResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "학기 관리 (회장단)", description = "동아리 활동 학기 전환 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/semesters")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSemesterController {

    private final SemesterService semesterService;
    private final SemesterTransitionService semesterTransitionService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "학기 전환 미리보기",
            description = "대상 학기로 전환했을 때의 영향(운영진 명단·해커톤 준비 여부)을 확인합니다.")
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<SemesterChangePreviewResponse>> preview(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "대상 연도") @RequestParam int actYear,
            @Parameter(description = "대상 학기 (1 또는 2)") @RequestParam int actSemester
    ) {
        staffAccountService.requirePresidentTeam(userId);
        SemesterChangePreview preview = semesterService.preview(actYear, actSemester);
        return ResponseEntity.ok(ApiResponse.success(SemesterChangePreviewResponse.from(preview)));
    }

    @Operation(summary = "활동 학기 변경",
            description = """
                    동아리의 현재 활동 학기를 변경합니다. 회장단만 사용할 수 있습니다.

                    변경 후 새로 생성되는 스터디 개설 신청·수강 신청이 이 학기로 기록되고,
                    목록·마이페이지의 현재 학기 판정이 바뀝니다. 이미 저장된 데이터는 영향받지 않습니다.

                    차기 회장 지정이 필수이며, 운영진(ADMIN) 계정을 가진 사람만 지정할 수 있습니다.
                    현 회장 본인을 지정하면 연임으로 처리되어 권한이 그대로 유지됩니다.
                    """)
    @PatchMapping("/current")
    public ResponseEntity<ApiResponse<SemesterResponse>> changeCurrentSemester(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangeSemesterRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        SemesterInfo changed = semesterTransitionService.transition(
                request.getActYear(), request.getActSemester(),
                request.getNextPresidentUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(SemesterResponse.from(changed)));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ChangeSemesterRequest {
        @NotNull
        @Min(2018)
        private Integer actYear;

        @NotNull
        @Min(1)
        @Max(2)
        private Integer actSemester;

        /** 차기 회장 학번 (필수). 연임이면 현 회장 본인을 지정한다. */
        @NotNull
        private Long nextPresidentUserId;
    }
}
