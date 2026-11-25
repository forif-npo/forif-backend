package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.StaffApplicationService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationRequests.SubmitRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationRequests.UpdateRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationResponse.DetailResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationResponse.SubmitResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 지원자 본인의 운영진 지원 API.
 * 로그인한 회원만 호출할 수 있고, 지원자 식별은 인증 컨텍스트에서만 얻는다.
 */
@Tag(name = "운영진 지원 (지원자)", description = "지원서 제출과 본인 지원 내역 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff-recruitments/{recruitmentId}/applications")
public class StaffApplicationController {

    private final StaffApplicationService staffApplicationService;

    @Operation(summary = "지원서 제출",
            description = "스터디 수강 이력이 있는 회원만 제출할 수 있습니다. 한 회차당 한 번만 제출됩니다.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SubmitResponse>> submit(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "모집 회차 ID") @PathVariable Integer recruitmentId,
            @Valid @RequestBody SubmitRequest request
    ) {
        SubmitResponse response = SubmitResponse.from(
                staffApplicationService.submit(recruitmentId, userId, request.toCommand()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "첨부파일을 포함한 지원서 제출")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SubmitResponse>> submitWithFiles(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestPart("request") SubmitRequest request,
            MultipartHttpServletRequest multipartRequest
    ) {
        Map<Integer, MultipartFile> files = attachmentParts(multipartRequest);
        SubmitResponse response = SubmitResponse.from(
                staffApplicationService.submit(recruitmentId, userId, request.toCommand(), files));
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "내 운영진 지원서 수정", description = "접수 중인 본인 지원서만 수정할 수 있습니다.")
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DetailResponse>> updateMine(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody UpdateRequest request
    ) {
        DetailResponse response = DetailResponse.from(staffApplicationService.update(
                recruitmentId, userId, request.toCommand(), Map.of(), removedFiles(request)), true);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "첨부파일을 포함한 내 운영진 지원서 수정")
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DetailResponse>> updateMineWithFiles(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestPart("request") UpdateRequest request,
            MultipartHttpServletRequest multipartRequest
    ) {
        DetailResponse response = DetailResponse.from(staffApplicationService.update(
                recruitmentId, userId, request.toCommand(), attachmentParts(multipartRequest), removedFiles(request)), true);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "내 운영진 지원 취소", description = "접수 중인 본인 지원서와 첨부파일을 삭제합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> cancelMine(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffApplicationService.cancel(recruitmentId, userId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(null));
    }

    private Set<Integer> removedFiles(UpdateRequest request) {
        return request.removedFileQuestionIds() == null
                ? Set.of() : new HashSet<>(request.removedFileQuestionIds());
    }

    private Map<Integer, MultipartFile> attachmentParts(MultipartHttpServletRequest multipartRequest) {
        Map<Integer, MultipartFile> files = new HashMap<>();
        multipartRequest.getMultiFileMap().forEach((partName, parts) -> {
            // FormData의 JSON Blob도 filename이 있어 MultipartFile 목록에 포함된다.
            if ("request".equals(partName)) {
                return;
            }
            if (!partName.matches("file_[1-9][0-9]*") || parts.size() != 1) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
            try {
                files.put(Integer.parseInt(partName.substring(5)), parts.get(0));
            } catch (NumberFormatException e) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
        });
        return files;
    }

    @Operation(summary = "내 지원서 첨부파일 다운로드", description = "본인이 제출한 파일 문항의 첨부파일만 받을 수 있습니다.")
    @GetMapping("/me/files/{questionId}")
    public ResponseEntity<byte[]> downloadMyFile(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer questionId
    ) {
        var detail = staffApplicationService.getMine(recruitmentId, userId);
        var attachment = staffApplicationService.downloadAttachment(detail, questionId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.filename(), StandardCharsets.UTF_8).build().toString())
                .body(attachment.bytes());
    }

    @Operation(summary = "본인 지원서 조회",
            description = "본인이 제출한 지원서만 반환합니다. 제출하지 않았다면 404입니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DetailResponse>> getMine(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        var application = staffApplicationService.getMine(recruitmentId, userId);
        DetailResponse response = DetailResponse.from(application,
                staffApplicationService.canModifyMine(recruitmentId, userId),
                staffApplicationService.getInterviewSummary(application));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }
}
