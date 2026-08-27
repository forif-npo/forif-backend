package org.forif_backend.web.userApply.dto;

import java.time.LocalDateTime;
import org.forif_backend.application.user.dto.AdminStudyApplicationInfo;
import org.forif_backend.common.dto.response.CursorPageResponse;

public record AdminStudyApplicationResponse(
        Long userId,
        String userName,
        String department,
        String studyName,
        int priority,
        LocalDateTime appliedAt
) {
    public static AdminStudyApplicationResponse from(AdminStudyApplicationInfo info) {
        return new AdminStudyApplicationResponse(info.userId(), info.userName(),
                info.department(), info.studyName(), info.priority(), info.appliedAt());
    }

    public static CursorPageResponse<AdminStudyApplicationResponse> fromPage(
            CursorPageResponse<AdminStudyApplicationInfo> page) {
        return page.withContent(page.content().stream().map(AdminStudyApplicationResponse::from).toList());
    }
}
