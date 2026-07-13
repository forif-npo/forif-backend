package org.forif_backend.application.study.dto;

import lombok.Builder;

import java.util.List;

/**
 * 수료증 발급 대상 조회 결과 (운영진용)
 */
@Builder
public record CertificateTargetsResult(
        Integer studyId,
        String studyName,
        int actYear,
        int actSemester,
        int requiredAttendance,
        List<Target> targets
) {
    @Builder
    public record Target(
            Long userId,
            String userName,
            String department,
            long attendanceCount,
            boolean hackathonParticipated,
            boolean eligible,
            int certificateStatus,
            String certificateUrl
    ) {
    }
}
