package org.forif_backend.application.staffrecruitment.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모집 회차 수정 입력.
 * departments·questions는 null이면 그대로 두고, 값이 있으면 전체를 교체한다.
 */
public record UpdateStaffRecruitmentCommand(
        Long version,
        String title,
        String summary,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<StaffRecruitmentDepartmentCommand> departments,
        List<StaffRecruitmentQuestionCommand> questions
) {
}
