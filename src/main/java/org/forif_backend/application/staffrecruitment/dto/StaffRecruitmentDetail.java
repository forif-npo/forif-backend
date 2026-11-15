package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.RecruitmentApplicationStatus;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모집 회차 조회 모델.
 * 접수 상태와 접수 가능 여부는 저장하지 않고 조회 시각으로 계산한다.
 */
public record StaffRecruitmentDetail(
        Integer recruitmentId,
        String title,
        String summary,
        String description,
        int actYear,
        int actSemester,
        String semesterLabel,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        StaffRecruitmentStatus publicationStatus,
        RecruitmentApplicationStatus applicationStatus,
        boolean applicationOpen,
        String applicationUnavailableReason,
        LocalDateTime endedAt,
        String closeReason,
        Long version,
        List<DepartmentInfo> departments,
        List<QuestionInfo> questions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record DepartmentInfo(
            Integer departmentId,
            String name,
            String description,
            int displayOrder,
            boolean active
    ) {
    }

    public record QuestionInfo(
            Integer questionId,
            String label,
            String description,
            StaffRecruitmentQuestionType type,
            boolean required,
            int maxLength,
            int displayOrder
    ) {
    }

    public static StaffRecruitmentDetail from(StaffRecruitment recruitment, LocalDateTime now) {
        RecruitmentApplicationStatus applicationStatus = recruitment.applicationStatus(now);
        return new StaffRecruitmentDetail(
                recruitment.getId(),
                recruitment.getTitle(),
                recruitment.getSummary(),
                recruitment.getDescription(),
                recruitment.getActYear(),
                recruitment.getActSemester(),
                semesterLabel(recruitment.getActYear(), recruitment.getActSemester()),
                recruitment.getStartsAt(),
                recruitment.getEndsAt(),
                recruitment.getStatus(),
                applicationStatus,
                applicationStatus.isOpen(),
                unavailableReason(recruitment, applicationStatus),
                recruitment.getEndedAt(),
                recruitment.getCloseReason(),
                recruitment.getVersion(),
                recruitment.getDepartments().stream()
                        .map(department -> new DepartmentInfo(
                                department.getId(),
                                department.getName(),
                                department.getDescription(),
                                department.getDisplayOrder(),
                                department.isActive()))
                        .toList(),
                recruitment.getQuestions().stream()
                        .map(question -> new QuestionInfo(
                                question.getId(),
                                question.getLabel(),
                                question.getDescription(),
                                question.getType(),
                                question.isRequired(),
                                question.getMaxLength(),
                                question.getDisplayOrder()))
                        .toList(),
                recruitment.getCreatedAt(),
                recruitment.getUpdatedAt()
        );
    }

    /** 화면에 쓰는 "27-1" 형태의 학기 표기 */
    private static String semesterLabel(int actYear, int actSemester) {
        return "%02d-%d".formatted(actYear % 100, actSemester);
    }

    /**
     * 접수가 불가능한 이유. 접수 중이면 null이다.
     * 프론트가 안내 문구를 고르는 데 쓰므로 값 집합을 계약에 고정한다.
     */
    private static String unavailableReason(StaffRecruitment recruitment, RecruitmentApplicationStatus status) {
        if (status.isOpen()) {
            return null;
        }
        if (recruitment.getStatus() == StaffRecruitmentStatus.CANCELED) {
            return "CANCELED";
        }
        if (recruitment.getStatus() == StaffRecruitmentStatus.DRAFT) {
            return "NOT_PUBLISHED";
        }
        if (recruitment.getStartsAt() == null || recruitment.getEndsAt() == null) {
            return "PERIOD_NOT_SET";
        }
        return status == RecruitmentApplicationStatus.UPCOMING ? "UPCOMING" : "CLOSED";
    }
}
