package org.forif_backend.web.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDetail;
import org.forif_backend.domain.staffrecruitment.RecruitmentApplicationStatus;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 공개 모집 응답.
 * 관리 전용 값(version, 마감 사유, 비활성 부서)은 포함하지 않는다.
 */
public record StaffRecruitmentResponse(
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
        List<DepartmentResponse> departments,
        List<QuestionResponse> questions,
        OffsetDateTime updatedAt
) {

    /** 계약상 응답 시각은 offset을 포함한 ISO 8601로 보낸다. */
    private static final ZoneId KOREA_STANDARD_TIME = ZoneId.of("Asia/Seoul");

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

    public static StaffRecruitmentResponse from(StaffRecruitmentDetail detail) {
        return new StaffRecruitmentResponse(
                detail.recruitmentId(),
                detail.title(),
                detail.summary(),
                detail.description(),
                detail.actYear(),
                detail.actSemester(),
                detail.semesterLabel(),
                toOffset(detail.startsAt()),
                toOffset(detail.endsAt()),
                detail.publicationStatus(),
                detail.applicationStatus(),
                detail.applicationOpen(),
                detail.applicationUnavailableReason(),
                detail.departments().stream()
                        // 모집을 닫은 부서는 공개 화면에서 제외한다. 과거 지원서의 참조는 그대로 둔다.
                        .filter(StaffRecruitmentDetail.DepartmentInfo::active)
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
                toOffset(detail.updatedAt())
        );
    }

    static OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(KOREA_STANDARD_TIME).toOffsetDateTime();
    }
}
