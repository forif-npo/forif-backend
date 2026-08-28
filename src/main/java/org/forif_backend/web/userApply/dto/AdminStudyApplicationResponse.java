package org.forif_backend.web.userApply.dto;

import java.time.LocalDateTime;
import org.forif_backend.application.user.dto.AdminStudyApplicationInfo;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.user.UserApplyStatus;

public record AdminStudyApplicationResponse(
        Long applicationId,
        Long userId,
        String userName,
        String department,
        Integer studyId,
        String studyName,
        int priority,
        UserApplyStatus status,
        boolean autonomousStudy,
        LocalDateTime appliedAt
) {
    public static AdminStudyApplicationResponse from(AdminStudyApplicationInfo info) {
        return new AdminStudyApplicationResponse(info.applicationId(), info.userId(), info.userName(),
                info.department(), info.studyId(), info.studyName(), info.priority(), info.status(),
                info.autonomousStudy(), info.appliedAt());
    }

    public static CursorPageResponse<AdminStudyApplicationResponse> fromPage(
            CursorPageResponse<AdminStudyApplicationInfo> page) {
        return page.withContent(page.content().stream().map(AdminStudyApplicationResponse::from).toList());
    }
}
