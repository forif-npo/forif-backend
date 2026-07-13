package org.forif_backend.web.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.forif_backend.application.study.dto.CertificateTargetsResult;

import java.util.List;

@Schema(description = "수료증 발급 대상 조회 응답")
@Builder
public record CertificateTargetsResponse(
        @Schema(description = "스터디 ID", example = "102")
        Integer studyId,

        @Schema(description = "스터디 이름", example = "README.md")
        String studyName,

        @Schema(description = "활동 연도", example = "2026")
        int actYear,

        @Schema(description = "활동 학기", example = "1")
        int actSemester,

        @Schema(description = "수료 기준 출석 횟수", example = "5")
        int requiredAttendance,

        @Schema(description = "멘티별 발급 대상 정보")
        List<Target> targets
) {
    @Builder
    public record Target(
            @Schema(description = "유저 ID(학번)", example = "2024097956")
            Long userId,

            @Schema(description = "이름", example = "홍길동")
            String userName,

            @Schema(description = "학과", example = "컴퓨터소프트웨어학부")
            String department,

            @Schema(description = "출석 횟수", example = "6")
            long attendanceCount,

            @Schema(description = "해당 학기 해커톤 참여 여부", example = "true")
            boolean hackathonParticipated,

            @Schema(description = "발급 자격 충족 여부 (출석 기준 + 해커톤 참여)", example = "true")
            boolean eligible,

            @Schema(description = "발급 상태 (0: 미발급, 1: 발급)", example = "1")
            int certificateStatus,

            @Schema(description = "발급된 수료증 URL (미발급 시 null)")
            String certificateUrl
    ) {
    }

    public static CertificateTargetsResponse from(CertificateTargetsResult result) {
        return CertificateTargetsResponse.builder()
                .studyId(result.studyId())
                .studyName(result.studyName())
                .actYear(result.actYear())
                .actSemester(result.actSemester())
                .requiredAttendance(result.requiredAttendance())
                .targets(result.targets().stream()
                        .map(t -> Target.builder()
                                .userId(t.userId())
                                .userName(t.userName())
                                .department(t.department())
                                .attendanceCount(t.attendanceCount())
                                .hackathonParticipated(t.hackathonParticipated())
                                .eligible(t.eligible())
                                .certificateStatus(t.certificateStatus())
                                .certificateUrl(t.certificateUrl())
                                .build())
                        .toList())
                .build();
    }
}
