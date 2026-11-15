package org.forif_backend.web.staffrecruitment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.forif_backend.application.staffrecruitment.dto.CreateStaffRecruitmentCommand;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDepartmentCommand;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentQuestionCommand;
import org.forif_backend.application.staffrecruitment.dto.UpdateStaffRecruitmentCommand;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 모집 관리 요청 본문 모음.
 * 부서·문항은 전체 목록을 보내 교체하는 방식이며, 시각은 offset 포함 ISO 8601로 받는다.
 */
public final class StaffRecruitmentRequests {

    private static final ZoneId KOREA_STANDARD_TIME = ZoneId.of("Asia/Seoul");

    private StaffRecruitmentRequests() {
    }

    public record DepartmentRequest(
            @NotBlank @Size(max = 50) String name,
            @Size(max = 200) String description,
            @Min(0) int displayOrder,
            Boolean active
    ) {
        StaffRecruitmentDepartmentCommand toCommand() {
            return new StaffRecruitmentDepartmentCommand(name, description, displayOrder, active == null || active);
        }
    }

    public record QuestionRequest(
            @NotBlank @Size(max = 100) String label,
            @Size(max = 200) String description,
            @NotNull StaffRecruitmentQuestionType type,
            Boolean required,
            @Min(1) @Max(5000) int maxLength,
            @Min(0) int displayOrder
    ) {
        StaffRecruitmentQuestionCommand toCommand() {
            return new StaffRecruitmentQuestionCommand(
                    label, description, type, required != null && required, maxLength, displayOrder);
        }
    }

    public record CreateRequest(
            @Min(2000) Integer actYear,
            @Min(1) @Max(2) Integer actSemester,
            @NotBlank @Size(max = 100) String title,
            @Size(max = 200) String summary,
            String description,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            @Valid List<DepartmentRequest> departments,
            @Valid List<QuestionRequest> questions
    ) {
        public CreateStaffRecruitmentCommand toCommand() {
            return new CreateStaffRecruitmentCommand(
                    actYear, actSemester, title, summary, description,
                    toLocal(startsAt), toLocal(endsAt),
                    departments == null ? List.of() : departments.stream().map(DepartmentRequest::toCommand).toList(),
                    questions == null ? List.of() : questions.stream().map(QuestionRequest::toCommand).toList());
        }
    }

    public record UpdateRequest(
            @NotNull Long version,
            @NotBlank @Size(max = 100) String title,
            @Size(max = 200) String summary,
            String description,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            @Valid List<DepartmentRequest> departments,
            @Valid List<QuestionRequest> questions
    ) {
        public UpdateStaffRecruitmentCommand toCommand() {
            return new UpdateStaffRecruitmentCommand(
                    version, title, summary, description,
                    toLocal(startsAt), toLocal(endsAt),
                    departments == null ? null : departments.stream().map(DepartmentRequest::toCommand).toList(),
                    questions == null ? null : questions.stream().map(QuestionRequest::toCommand).toList());
        }
    }

    /** publish·close·cancel 공통 본문. publish는 reason을 쓰지 않는다. */
    public record ActionRequest(
            @NotNull Long version,
            @Size(max = 200) String reason
    ) {
    }

    private static LocalDateTime toLocal(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KOREA_STANDARD_TIME).toLocalDateTime();
    }
}
