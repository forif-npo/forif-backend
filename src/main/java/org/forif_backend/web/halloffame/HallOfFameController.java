package org.forif_backend.web.halloffame;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.halloffame.HallOfFameService;
import org.forif_backend.application.halloffame.dto.HallOfFameEntryDetail;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.web.halloffame.dto.HallOfFameResponse.PublicResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 공개 명예의 전당 조회. 게시된 항목만 반환한다.
 */
@Tag(name = "명예의 전당 (공개)", description = "게시된 전시 항목 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hall-of-fame")
public class HallOfFameController {

    private static final int MAX_PAGE_SIZE = 100;
    /** 게시 중단이 60초 안에 반영되도록 공개 캐시 수명을 맞춘다. */
    private static final Duration PUBLIC_CACHE = Duration.ofSeconds(60);

    private final HallOfFameService hallOfFameService;

    @Operation(summary = "명예의 전당 목록",
            description = "게시된 항목만 반환합니다. 시즌으로 걸러 볼 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<PublicResponse>>> getList(
            @Parameter(description = "활동 연도") @RequestParam(required = false) Integer actYear,
            @Parameter(description = "활동 학기 (1 또는 2)") @RequestParam(required = false) Integer actSemester,
            @Parameter(description = "0부터 시작하는 페이지") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(defaultValue = "20") int size
    ) {
        CursorPageResponse<HallOfFameEntryDetail> result =
                hallOfFameService.getPublished(actYear, actSemester, Math.max(page, 0), clampSize(size));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(PUBLIC_CACHE).cachePublic())
                .body(ApiResponse.success(
                        result.withContent(result.content().stream().map(PublicResponse::from).toList())));
    }

    @Operation(summary = "명예의 전당 상세",
            description = "게시된 항목만 반환합니다. 초안과 게시 중단은 주소를 알아도 404입니다.")
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<PublicResponse>> getDetail(
            @Parameter(description = "전시 항목 주소") @PathVariable String slug
    ) {
        PublicResponse response = PublicResponse.from(hallOfFameService.getPublishedBySlug(slug));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(PUBLIC_CACHE).cachePublic())
                .body(ApiResponse.success(response));
    }

    private int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
