package org.forif_backend.web.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 스터디 거절 요청 DTO
 * UI에서 입력한 직접 입력 사유만 담습니다.
 */
public record StudyRejectRequest(
        @NotBlank(message = "거절 사유는 필수입니다.")
        @Size(max = 1000, message = "거절 사유는 1000자 이내로 작성해주세요.")
        String reason
) {}