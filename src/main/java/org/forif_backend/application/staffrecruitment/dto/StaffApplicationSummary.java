package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회장단 지원서 목록 행.
 * 목록 단계에서는 연락처와 답변을 내려주지 않는다.
 */
public record StaffApplicationSummary(
        Integer applicationId,
        String applicantName,
        String applicantDepartment,
        List<StaffApplicationDetail.AppliedDepartment> appliedDepartments,
        StaffApplicationStatus status,
        LocalDateTime submittedAt
) {

    public static StaffApplicationSummary from(StaffApplication application) {
        return new StaffApplicationSummary(
                application.getId(),
                application.getApplicantName(),
                application.getApplicantDepartment(),
                application.getChoices().stream()
                        .map(choice -> new StaffApplicationDetail.AppliedDepartment(
                                choice.getDepartment().getId(),
                                choice.getDepartment().getName(),
                                choice.getPriority()))
                        .toList(),
                application.getStatus(),
                application.getSubmittedAt()
        );
    }
}
