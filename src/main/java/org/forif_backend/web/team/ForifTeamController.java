package org.forif_backend.web.team;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.team.ForifTeamService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.web.team.dto.ForifTeamResponse;
import org.forif_backend.web.team.dto.UpdateForifTeamRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "포리프 팀", description = "역대 운영진 이력 API")
@RestController
@RequiredArgsConstructor
public class ForifTeamController {

    private final ForifTeamService forIfTeamService;

    @Operation(summary = "전체 운영진 이력 조회")
    @GetMapping("/api/v1/forif-team")
    public ResponseEntity<ApiResponse<List<ForifTeamResponse>>> getAllMembers() {
        List<ForifTeamResponse> response = forIfTeamService.getAllMembers().stream()
                .map(ForifTeamResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "학기별 운영진 이력 조회")
    @GetMapping("/api/v1/forif-team/{year}/{semester}")
    public ResponseEntity<ApiResponse<List<ForifTeamResponse>>> getMembersByYearAndSemester(
            @Parameter(description = "연도 (예: 2025)") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)") @PathVariable int semester
    ) {
        List<ForifTeamResponse> response = forIfTeamService.getMembersByYearAndSemester(year, semester).stream()
                .map(ForifTeamResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "운영진 이력 수정 (어드민 전용)", description = "직책, 팀, 소개 등 운영진 프로필 정보를 수정합니다.")
    @PatchMapping("/api/v1/admin/forif-team/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ForifTeamResponse>> updateMember(
            @Parameter(description = "운영진 이력 ID") @PathVariable Long id,
            @RequestBody UpdateForifTeamRequest request
    ) {
        ForifTeam updated = forIfTeamService.updateMember(
                id,
                request.getUserTitle(),
                request.getClubDepartment(),
                request.getIntroTag(),
                request.getSelfIntro(),
                request.getProfImgUrl(),
                request.getGraduateYear()
        );
        return ResponseEntity.ok(ApiResponse.success(ForifTeamResponse.from(updated)));
    }

    @Operation(summary = "운영진 이력 삭제 (어드민 전용)")
    @DeleteMapping("/api/v1/admin/forif-team/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @Parameter(description = "운영진 이력 ID") @PathVariable Long id
    ) {
        forIfTeamService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
