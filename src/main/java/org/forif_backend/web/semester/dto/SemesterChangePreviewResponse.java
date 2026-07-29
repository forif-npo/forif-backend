package org.forif_backend.web.semester.dto;

import org.forif_backend.application.semester.dto.SemesterChangePreview;

public record SemesterChangePreviewResponse(
        SemesterResponse current,
        SemesterResponse target,
        int targetTeamMemberCount,
        boolean needsTeamSetup,
        boolean targetHackathonExists,
        long currentMemberCount,
        long currentCertificateIssuedCount,
        boolean hasPendingCertificates
) {
    public static SemesterChangePreviewResponse from(SemesterChangePreview preview) {
        return new SemesterChangePreviewResponse(
                SemesterResponse.from(preview.current()),
                SemesterResponse.from(preview.target()),
                preview.targetTeamMemberCount(),
                preview.needsTeamSetup(),
                preview.targetHackathonExists(),
                preview.currentMemberCount(),
                preview.currentCertificateIssuedCount(),
                preview.hasPendingCertificates()
        );
    }
}
