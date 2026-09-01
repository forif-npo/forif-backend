package org.forif_backend.application.user.dto;

/**
 * 인증서 조회 Result
 * Application 계층 DTO
 */
public record GetCertificateResult(
    String certificateUrl
) {
}
