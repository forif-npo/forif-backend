package org.forif_backend.web.study;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.CertificateService;
import org.forif_backend.application.study.dto.CertificateTargetsResult;
import org.forif_backend.application.study.dto.IssueCertificatesResult;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.study.dto.CertificateTargetsResponse;
import org.forif_backend.web.study.dto.IssueCertificatesRequest;
import org.forif_backend.web.study.dto.IssueCertificatesResponse;
import org.forif_backend.web.study.dto.ManualCertificateRequest;
import org.forif_backend.web.study.dto.ManualCertificateResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "수료증 발급 (어드민)", description = "운영진의 스터디 수료증 발급 API")
@RestController
@RequiredArgsConstructor
public class AdminCertificateController {

    private final CertificateService certificateService;

    @Operation(summary = "수료증 발급 대상 조회 (어드민 전용)",
            description = "스터디 멘티 전원의 출석 횟수, 해커톤 참여 여부, 발급 자격, 발급 상태를 조회합니다. 발급 자격: 출석 5회 이상 + 해당 학기 해커톤 참가 등록.")
    @GetMapping("/api/v1/admin/studies/{studyId}/certificates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CertificateTargetsResponse>> getCertificateTargets(
            @Parameter(description = "스터디 ID") @PathVariable Integer studyId
    ) {
        CertificateTargetsResult result = certificateService.getCertificateTargets(studyId);
        return ResponseEntity.ok(ApiResponse.success(CertificateTargetsResponse.from(result)));
    }

    @Operation(summary = "수료증 발급 (어드민 전용)",
            description = "선택한 멘티들의 수료증 이미지를 생성해 파일 저장소에 저장하고 발급 상태를 갱신합니다. 자격 미달자는 스킵되며 결과에 사유가 담깁니다. 이미 발급된 유저는 재발급됩니다.")
    @PostMapping("/api/v1/admin/studies/{studyId}/certificates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IssueCertificatesResponse>> issueCertificates(
            @Parameter(description = "스터디 ID") @PathVariable Integer studyId,
            @Valid @RequestBody IssueCertificatesRequest request
    ) {
        IssueCertificatesResult result = certificateService.issueCertificates(
                studyId, request.userIds(), request.activityPeriod(),
                Boolean.TRUE.equals(request.ignoreEligibility()));
        return ResponseEntity.ok(ApiResponse.success(IssueCertificatesResponse.from(result)));
    }

    @Operation(summary = "수료증 수동 발급 (어드민 전용)",
            description = "특수 케이스를 위해 이름, 학번, 학과, 스터디명, 활동 기간을 직접 입력해 수료증을 생성합니다. 자격 검증과 DB 기록 없이 이미지 URL만 반환합니다.")
    @PostMapping("/api/v1/admin/certificates/manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ManualCertificateResponse>> issueManualCertificate(
            @Valid @RequestBody ManualCertificateRequest request
    ) {
        String certificateUrl = certificateService.issueManualCertificate(
                request.userName(),
                request.studentNumber(),
                request.department(),
                request.studyName(),
                request.activityPeriod(),
                request.issueDate(),
                request.presidentName()
        );
        return ResponseEntity.ok(ApiResponse.success(new ManualCertificateResponse(certificateUrl)));
    }

    @Operation(summary = "내 서명 조회 (어드민 전용)",
            description = "로그인한 운영진 본인이 등록한 수료증 서명 이미지 URL을 조회합니다. 미등록 시 null.")
    @GetMapping("/api/v1/admin/certificates/signature")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SignatureResponse>> getSignature(
            @AuthenticationPrincipal Long userId
    ) {
        String signatureUrl = certificateService.getSignatureUrl(userId);
        return ResponseEntity.ok(ApiResponse.success(new SignatureResponse(signatureUrl)));
    }

    @Operation(summary = "내 서명 등록 (어드민 전용)",
            description = "로그인한 운영진 본인의 수료증 서명 이미지를 등록합니다. 투명 배경 PNG 권장. 현재 회장의 서명이 수료증에 합성됩니다.")
    @PostMapping(value = "/api/v1/admin/certificates/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SignatureResponse>> uploadSignature(
            @AuthenticationPrincipal Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        String signatureUrl = certificateService.uploadSignature(userId, file);
        return ResponseEntity.ok(ApiResponse.success(new SignatureResponse(signatureUrl)));
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "서명 조회/등록 응답")
    public record SignatureResponse(
            @io.swagger.v3.oas.annotations.media.Schema(description = "서명 이미지 URL (미등록 시 null)")
            String signatureUrl
    ) {
    }
}
