package org.forif_backend.web.post;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.post.PostService;
import org.forif_backend.application.post.dto.PostDto;
import org.forif_backend.application.post.dto.PostListResult;
import org.forif_backend.common.dto.request.PageRequest;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.post.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    // 자주 묻는 질문 반환
    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<List<FAQResponse>>> getFAQs(
            @ModelAttribute PageRequest pageRequest,
            @RequestParam(required = false) String search
    ) {
        PostListResult result = postService.getFAQs(pageRequest.getPage(), pageRequest.getPageSize(), search);
        List<FAQResponse> response = result.posts().stream()
                .map(PostDtoMapper::toFAQResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 자주 묻는 질문 생성 (관리자)
    @PostMapping("/faqs")
    public ResponseEntity<ApiResponse<Void>> createFAQ(
            @RequestBody FAQRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        postService.createFAQ(userId, request.title(), request.content(), request.tag());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 자주 묻는 질문 수정 (관리자)
    @PatchMapping("/faqs/{id}")
    public ResponseEntity<ApiResponse<Void>> updateFAQ(
            @PathVariable Integer id,
            @RequestBody FAQRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        postService.updateFAQ(userId, id, request.title(), request.content(), request.tag());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 자주 묻는 질문 삭제 (관리자)
    @DeleteMapping("/faqs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFAQ(
            @PathVariable Integer id,
            @AuthenticationPrincipal Long userId
    ) {
        postService.deleteFAQ(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 공지사항 반환
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getAnnouncements(
            @ModelAttribute PageRequest pageRequest,
            @RequestParam(required = false) String search
    ) {
        PostListResult result = postService.getAnnouncements(pageRequest.getPage(), pageRequest.getPageSize(), search);
        List<AnnouncementResponse> response = result.posts().stream()
                .map(PostDtoMapper::toAnnouncementResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 공지사항(단일) 반환
    @GetMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(
            @PathVariable Integer id
    ) {
        PostDto result = postService.getAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(PostDtoMapper.toAnnouncementResponse(result)));
    }

    // 공지사항 생성 (관리자)
    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<Void>> createAnnouncement(
            @RequestPart("request") AnnouncementRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @AuthenticationPrincipal Long userId
    ) {
        postService.createAnnouncement(userId, request.title(), request.content(), request.tag(), images);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 공지사항 수정 (관리자)
    @PatchMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> updateAnnouncement(
            @PathVariable Integer id,
            @RequestPart("request") AnnouncementUpdateRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @AuthenticationPrincipal Long userId
    ) {
        postService.updateAnnouncement(userId, id, request.title(), request.content(), request.tag(), images);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 공지사항 삭제 (관리자)
    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable Integer id,
            @AuthenticationPrincipal Long userId
    ) {
        postService.deleteAnnouncement(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
