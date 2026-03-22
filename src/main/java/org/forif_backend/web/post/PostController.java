package org.forif_backend.web.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.post.PostService;
import org.forif_backend.application.post.dto.PostDto;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.web.post.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "게시글", description = "FAQ 및 공지사항 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    // 자주 묻는 질문 반환
    @Operation(summary = "FAQ 목록 조회", description = "커서 기반 페이지네이션으로 자주 묻는 질문 목록을 조회합니다.")
    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<CursorPageResponse<FAQResponse>>> getFAQs(
            @Parameter(description = "이전 페이지의 마지막 FAQ ID. 최초 조회 시 생략") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "검색어 (제목)") @RequestParam(required = false) String search
    ) {
        CursorPageResponse<PostDto> result = postService.getFAQs(cursor, size, search);
        List<FAQResponse> content = result.content().stream()
                .map(PostDtoMapper::toFAQResponse)
                .toList();
        CursorPageResponse<FAQResponse> response = new CursorPageResponse<>(
                content, result.nextCursor(), result.hasNext(), result.totalElements()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 자주 묻는 질문 생성 (관리자)
    @Operation(summary = "FAQ 생성 (어드민 전용)", description = "새 FAQ를 등록합니다.")
    @PostMapping("/faqs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> createFAQ(
            @RequestBody FAQRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        postService.createFAQ(userId, request.title(), request.content(), request.tag());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 자주 묻는 질문 수정 (관리자)
    @Operation(summary = "FAQ 수정 (어드민 전용)", description = "기존 FAQ의 내용을 수정합니다.")
    @PatchMapping("/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateFAQ(
            @Parameter(description = "수정할 FAQ ID") @PathVariable Integer id,
            @RequestBody FAQRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        postService.updateFAQ(userId, id, request.title(), request.content(), request.tag());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 자주 묻는 질문 삭제 (관리자)
    @Operation(summary = "FAQ 삭제 (어드민 전용)", description = "FAQ를 삭제합니다.")
    @DeleteMapping("/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFAQ(
            @Parameter(description = "삭제할 FAQ ID") @PathVariable Integer id,
            @AuthenticationPrincipal Long userId
    ) {
        postService.deleteFAQ(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 공지사항 반환
    @Operation(summary = "공지사항 목록 조회", description = "커서 기반 페이지네이션으로 공지사항 목록을 조회합니다.")
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<CursorPageResponse<AnnouncementResponse>>> getAnnouncements(
            @Parameter(description = "이전 페이지의 마지막 공지사항 ID. 최초 조회 시 생략") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "검색어 (제목)") @RequestParam(required = false) String search
    ) {
        CursorPageResponse<PostDto> result = postService.getAnnouncements(cursor, size, search);
        List<AnnouncementResponse> content = result.content().stream()
                .map(PostDtoMapper::toAnnouncementResponse)
                .toList();
        CursorPageResponse<AnnouncementResponse> response = new CursorPageResponse<>(
                content, result.nextCursor(), result.hasNext(), result.totalElements()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 공지사항(단일) 반환
    @Operation(summary = "공지사항 단건 조회", description = "특정 공지사항의 상세 내용을 조회합니다.")
    @GetMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(
            @Parameter(description = "조회할 공지사항 ID") @PathVariable Integer id
    ) {
        PostDto result = postService.getAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(PostDtoMapper.toAnnouncementResponse(result)));
    }

    // 공지사항 생성 (관리자)
    @Operation(summary = "공지사항 생성 (어드민 전용)", description = "새 공지사항을 등록합니다. 이미지 파일을 함께 첨부할 수 있습니다.")
    @PostMapping("/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> createAnnouncement(
            @RequestPart("request") AnnouncementRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @AuthenticationPrincipal Long userId
    ) {
        postService.createAnnouncement(userId, request.title(), request.content(), request.tag(), images);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 공지사항 수정 (관리자)
    @Operation(summary = "공지사항 수정 (어드민 전용)", description = "기존 공지사항의 내용을 수정합니다.")
    @PatchMapping("/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateAnnouncement(
            @Parameter(description = "수정할 공지사항 ID") @PathVariable Integer id,
            @RequestPart("request") AnnouncementUpdateRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @AuthenticationPrincipal Long userId
    ) {
        postService.updateAnnouncement(userId, id, request.title(), request.content(), request.tag(), images);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 공지사항 삭제 (관리자)
    @Operation(summary = "공지사항 삭제 (어드민 전용)", description = "공지사항을 삭제합니다.")
    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @Parameter(description = "삭제할 공지사항 ID") @PathVariable Integer id,
            @AuthenticationPrincipal Long userId
    ) {
        postService.deleteAnnouncement(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
