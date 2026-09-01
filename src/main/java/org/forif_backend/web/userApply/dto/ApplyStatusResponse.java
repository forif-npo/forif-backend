package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.forif_backend.application.user.dto.ApplyStatusInfo;
import org.forif_backend.web.study.dto.StudyResponse;

@Schema(description = "지원 상태 응답")
@Builder
public record ApplyStatusResponse(
        @Schema(description = "멘티 모집 기간이 열려 있고 1순위 지원 이력이 없을 때의 1순위 지원 가능 여부", example = "true")
        boolean canApplyPrimary,

        @Schema(description = "멘티 모집 기간이 열려 있고 2순위 지원 이력이 없을 때의 2순위 지원 가능 여부", example = "true")
        boolean canApplySecondary,

        @Schema(description = "멘티 모집 기간이 열려 있고 이번 학기 스터디 신청 이력이 없을 때의 자율부원 신청 가능 여부", example = "true")
        boolean canApplyAutonomousStudy,

        @Schema(description = "이번 학기 자율부원 신청 이력 보유 여부", example = "false")
        boolean hasAutonomousStudyApplication,

        @Schema(description = "이미 지원한 1순위 스터디 정보")
        StudyResponse primaryStudy,

        @Schema(description = "이미 지원한 2순위 스터디 정보")
        StudyResponse secondaryStudy
) {
    public static ApplyStatusResponse from(ApplyStatusInfo info) {
        return ApplyStatusResponse.builder()
                .canApplyPrimary(info.canApplyPrimary())
                .canApplySecondary(info.canApplySecondary())
                .canApplyAutonomousStudy(info.canApplyAutonomousStudy())
                .hasAutonomousStudyApplication(info.hasAutonomousStudyApplication())
                .primaryStudy(info.primaryStudy() == null ? null : StudyResponse.from(info.primaryStudy()))
                .secondaryStudy(info.secondaryStudy() == null ? null : StudyResponse.from(info.secondaryStudy()))
                .build();
    }
}
