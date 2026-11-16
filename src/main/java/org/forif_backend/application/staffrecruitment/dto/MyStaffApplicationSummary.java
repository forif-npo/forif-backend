package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationStatus;

import java.time.LocalDateTime;

public record MyStaffApplicationSummary(
        Integer applicationId,
        Integer recruitmentId,
        int actYear,
        int actSemester,
        StaffApplicationStatus status,
        LocalDateTime submittedAt
) {
    public static MyStaffApplicationSummary from(StaffApplication application) {
        return new MyStaffApplicationSummary(
                application.getId(),
                application.getRecruitment().getId(),
                application.getRecruitment().getActYear(),
                application.getRecruitment().getActSemester(),
                application.getStatus(),
                application.getSubmittedAt());
    }
}
