package org.forif_backend.web.halloffame;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.halloffame.HallOfFameService;
import org.forif_backend.application.halloffame.dto.HallOfFameEntryDetail;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.halloffame.HallOfFameStatus;
import org.forif_backend.web.halloffame.dto.HallOfFameRequests.ActionRequest;
import org.forif_backend.web.halloffame.dto.HallOfFameRequests.CreateRequest;
import org.forif_backend.web.halloffame.dto.HallOfFameRequests.UpdateRequest;
import org.forif_backend.web.halloffame.dto.HallOfFameResponse.AdminResponse;
import org.forif_backend.web.halloffame.dto.HallOfFameResponse.ImageResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 명예의 전당 전시 관리. 회장단만 등록·수정·게시할 수 있다.
 * 미리보기는 관리 상세 응답을 그대로 쓰므로 초안도 이 경로로만 볼 수 있다.
 */
@Tag(name = "명예의 전당 관리 (회장단)", description = "전시 항목 등록·수정·게시")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/hall-of-fame")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHallOfFameController {

    private static final int MAX_PAGE_SIZE = 100;

    private final HallOfFameService hallOfFameService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "전시 항목 목록", description = "초안과 게시 중단을 포함한 전체 목록입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminResponse>>> getList(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게시 상태 필터") @RequestParam(required = false) HallOfFameStatus status,
            @Parameter(description = "0부터 시작하는 페이지") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(defaultValue = "20") int size
    ) {
        staffAccountService.requirePresidentTeam(userId);

        CursorPageResponse<HallOfFameEntryDetail> result =
                hallOfFameService.getAdminList(status, Math.max(page, 0), clampSize(size));

        return noStore(ApiResponse.success(
                result.withContent(result.content().stream().map(AdminResponse::from).toList())));
    }

    @Operation(summary = "전시 항목 생성", description = "초안으로 만듭니다. 게시는 별도 요청입니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);

        AdminResponse response = AdminResponse.from(hallOfFameService.create(request.toCommand()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "전시 항목 상세·미리보기", description = "초안과 게시 중단 항목도 볼 수 있습니다.")
    @GetMapping("/{entryId}")
    public ResponseEntity<ApiResponse<AdminResponse>> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer entryId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(ApiResponse.success(AdminResponse.from(hallOfFameService.getForAdmin(entryId))));
    }

    @Operation(summary = "전시 항목 수정", description = "version이 다르면 409로 거절합니다.")
    @PatchMapping("/{entryId}")
    public ResponseEntity<ApiResponse<AdminResponse>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer entryId,
            @Valid @RequestBody UpdateRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(ApiResponse.success(
                AdminResponse.from(hallOfFameService.update(entryId, request.toCommand()))));
    }

    @Operation(summary = "전시 이미지 업로드", description = "기존 이미지는 교체 후 정리됩니다. 5MB 이하 이미지만 허용합니다.")
    @PostMapping(value = "/{entryId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageResponse>> uploadImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer entryId,
            @RequestPart("file") MultipartFile file
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(ApiResponse.success(new ImageResponse(hallOfFameService.uploadImage(entryId, file))));
    }

    @Operation(summary = "전시 항목 게시", description = "공개 목록과 상세에 나타납니다.")
    @PostMapping("/{entryId}/publish")
    public ResponseEntity<ApiResponse<AdminResponse>> publish(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer entryId,
            @Valid @RequestBody ActionRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(ApiResponse.success(
                AdminResponse.from(hallOfFameService.publish(entryId, request.version()))));
    }

    @Operation(summary = "전시 항목 게시 중단", description = "공개 조회에서 제외됩니다. 기록은 남습니다.")
    @PostMapping("/{entryId}/unpublish")
    public ResponseEntity<ApiResponse<AdminResponse>> unpublish(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer entryId,
            @Valid @RequestBody ActionRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(ApiResponse.success(
                AdminResponse.from(hallOfFameService.unpublish(entryId, request.version()))));
    }

    private int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(body);
    }
}
