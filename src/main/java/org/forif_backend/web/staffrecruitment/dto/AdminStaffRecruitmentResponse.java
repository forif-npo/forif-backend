package org.forif_backend.web.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDetail;
import org.forif_backend.domain.staffrecruitment.RecruitmentApplicationStatus;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 회장단 관리 응답. 초안과 비활성 부서까지 보여 주고 낙관적 잠금 version을 포함한다.
 */
public record AdminStaffRecruitmentResponse(
        Integer recruitmentId,
        String title,
        String summary,
        String description,
        int actYear,
        int actSemester,
        String semesterLabel,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        StaffRecruitmentStatus publicationStatus,
        RecruitmentApplicationStatus applicationStatus,
        boolean applicationOpen,
        String applicationUnavailableReason,
        OffsetDateTime endedAt,
        String closeReason,
        Long version,
        List<DepartmentResponse> departments,
        List<QuestionResponse> questions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record DepartmentResponse(
            Integer departmentId,
            String name,
            String description,
            int displayOrder,
            boolean active
    ) {
    }

    public record QuestionResponse(
            Integer questionId,
            String label,
            String description,
            StaffRecruitmentQuestionType type,
            boolean required,
            int maxLength,
            int displayOrder
    ) {
    }

    public static AdminStaffRecruitmentResponse from(StaffRecruitmentDetail detail) {
        return new AdminStaffRecruitmentResponse(
                detail.recruitmentId(),
                detail.title(),
                detail.summary(),
                detail.description(),
                detail.actYear(),
                detail.actSemester(),
                detail.semesterLabel(),
                StaffRecruitmentResponse.toOffset(detail.startsAt()),
                StaffRecruitmentResponse.toOffset(detail.endsAt()),
                detail.publicationStatus(),
                detail.applicationStatus(),
                detail.applicationOpen(),
                detail.applicationUnavailableReason(),
                StaffRecruitmentResponse.toOffset(detail.endedAt()),
                detail.closeReason(),
                detail.version(),
                detail.departments().stream()
                        .map(department -> new DepartmentResponse(
                                department.departmentId(),
                                department.name(),
                                department.description(),
                                department.displayOrder(),
                                department.active()))
                        .toList(),
                detail.questions().stream()
                        .map(question -> new QuestionResponse(
                                question.questionId(),
                                question.label(),
                                question.description(),
                                question.type(),
                                question.required(),
                                question.maxLength(),
                                question.displayOrder()))
                        .toList(),
                StaffRecruitmentResponse.toOffset(detail.createdAt()),
                StaffRecruitmentResponse.toOffset(detail.updatedAt())
        );
    }
}
