package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.forif_backend.application.user.dto.UserApplyInfo;

import java.time.LocalDateTime;

@Schema(description = "신청자 목록 응답")
@Builder
public record UserApplyResponse(
        @Schema(description = "신청서 ID", example = "42")
        Long applyId,

        @Schema(description = "신청자 이름", example = "홍길동")
        String applierName,

        @Schema(description = "신청한 스터디 이름", example = "Spring Boot 스터디")
        String studyName,

        @Schema(description = "지원 동기 요약 (첫 100자)", example = "이 스터디에 지원하는 이유는...")
        String studyComment,

        @Schema(description = "신청 일시", example = "2025-03-01T10:00:00")
        LocalDateTime applyDate,

        @Schema(description = "현재 신청 상태 (대기중 / 승낙 / 거절 / 예비)", example = "대기중")
        String studyStatus
) {
    public static UserApplyResponse from(UserApplyInfo userApplyInfo) {
        return UserApplyResponse.builder()
                .applyId(userApplyInfo.applyId())
                .applierName(userApplyInfo.applierName())
                .applyDate(userApplyInfo.applyDate())
                .studyName(userApplyInfo.studyName())
                .studyComment(userApplyInfo.studyComment())
                .studyStatus(userApplyInfo.studyStatus())
                .build();
    }
}
