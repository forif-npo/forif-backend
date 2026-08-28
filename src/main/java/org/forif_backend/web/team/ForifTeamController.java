package org.forif_backend.web.team;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.team.ForifTeamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.team.dto.ForifTeamResponse;
import org.forif_backend.web.team.dto.UpdateForifTeamRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "포리프 팀", description = "역대 운영진 이력 API")
@RestController
@RequiredArgsConstructor
public class ForifTeamController {

    private final ForifTeamService forifTeamService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "전체 운영진 이력 조회")
    @GetMapping("/api/v1/forif-team")
    public ResponseEntity<ApiResponse<List<ForifTeamResponse>>> getAllMembers() {
        List<ForifTeamResponse> response = forifTeamService.getAllMembers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "학기별 운영진 이력 조회")
    @GetMapping("/api/v1/forif-team/{year}/{semester}")
    public ResponseEntity<ApiResponse<List<ForifTeamResponse>>> getMembersByYearAndSemester(
            @Parameter(description = "연도 (예: 2025)") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)") @PathVariable int semester
    ) {
        List<ForifTeamResponse> response = forifTeamService.getMembersByYearAndSemester(year, semester);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "운영진 명단 추가 (회장단 전용)",
            description = """
                    운영진 명단(홈페이지 운영진 소개)에 부원을 추가합니다.

                    학기를 지정하지 않으면 현재 활동 학기에 추가됩니다.
                    학기가 바뀌면 명단은 자동으로 이어지지 않으므로, 새 학기마다 이 API로 지정해야 합니다.
                    """)
    @PostMapping("/api/v1/admin/forif-team")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ForifTeamResponse>> addMember(
            @AuthenticationPrincipal Long requesterId,
            @Valid @RequestBody AddForifTeamRequest request
    ) {
        staffAccountService.requirePresidentTeam(requesterId);
        ForifTeamResponse response = forifTeamService.addMember(
                request.getUserId(),
                request.getActYear(),
                request.getActSemester(),
                request.getClubDepartment(),
                request.getUserTitle()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AddForifTeamRequest {
        @NotNull
        private Long userId;

        /** 미지정 시 현재 활동 학기 */
        private Integer actYear;
        private Integer actSemester;

        @NotBlank
        private String clubDepartment;

        /** 회장/부회장/팀장 등 직책 (선택) */
        private String userTitle;
    }

    @Operation(summary = "운영진 이력 수정 (어드민 전용)", description = "직책, 팀, 소개 등 운영진 프로필 정보를 수정합니다.")
    @PatchMapping("/api/v1/admin/forif-team/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ForifTeamResponse>> updateMember(
            @Parameter(description = "운영진 이력 ID") @PathVariable Long id,
            @RequestBody UpdateForifTeamRequest request
    ) {
        ForifTeamResponse response = forifTeamService.updateMember(
                id,
                request.userTitle(),
                request.clubDepartment(),
                request.introTag(),
                request.selfIntro(),
                request.profImgUrl(),
                request.graduateYear()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "운영진 프로필 사진 등록·교체",
            description = "운영진 소개 페이지에 표시할 5MB 이하 JPEG 또는 PNG 프로필 사진을 등록하거나 교체합니다.")
    @PatchMapping(value = "/api/v1/admin/forif-team/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ForifTeamResponse>> updateMemberProfileImage(
            @Parameter(description = "운영진 이력 ID") @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(forifTeamService.updateMemberProfileImage(id, file)));
    }

    @Operation(summary = "운영진 이력 삭제 (어드민 전용)")
    @DeleteMapping("/api/v1/admin/forif-team/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @Parameter(description = "운영진 이력 ID") @PathVariable Long id
    ) {
        forifTeamService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
