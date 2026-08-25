package org.forif_backend.application.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

/**
 * 스터디 수강 신청서 수정.
 *
 * <p>제약을 컨트롤러의 @Valid가 아니라 이 커맨드에 두는 이유: 자율스터디 수정 거부가
 * 입력 형식 검증보다 먼저 반환되어야 해서(커밋 1675760) 서비스가 검증 시점을 직접 정한다.
 */
public record UserApplyUpdateCommand(
        @NotNull
        Integer studyId,

        @NotBlank
        @Length(min = 50, max = 500, message = "지원 사유는 50자 이상 500자 이내로 작성해주세요.")
        String applyReason,

        @NotNull
        @Min(value = 1, message = "지원 순위는 1 또는 2만 가능합니다.")
        @Max(value = 2, message = "지원 순위는 1 또는 2만 가능합니다.")
        Integer priority
) {
}
