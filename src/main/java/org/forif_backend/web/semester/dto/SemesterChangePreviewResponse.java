package org.forif_backend.web.semester.dto;

import org.forif_backend.application.semester.dto.SemesterChangePreview;

public record SemesterChangePreviewResponse(
        SemesterResponse current,
        SemesterResponse target,
        int targetTeamMemberCount,
        int currentTeamMemberCount,
        boolean needsTeamCopy,
        boolean targetHackathonExists
) {
    public static SemesterChangePreviewResponse from(SemesterChangePreview preview) {
        return new SemesterChangePreviewResponse(
                SemesterResponse.from(preview.current()),
                SemesterResponse.from(preview.target()),
                preview.targetTeamMemberCount(),
                preview.currentTeamMemberCount(),
                preview.needsTeamCopy(),
                preview.targetHackathonExists()
        );
    }
}
