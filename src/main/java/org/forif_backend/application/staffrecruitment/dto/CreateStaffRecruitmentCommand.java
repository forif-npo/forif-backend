package org.forif_backend.application.staffrecruitment.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모집 회차 생성 입력.
 * 활동 학기를 지정하지 않으면 현재 활동 학기를 값으로 복사해 보관한다.
 */
public record CreateStaffRecruitmentCommand(
        Integer actYear,
        Integer actSemester,
        String title,
        String summary,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<StaffRecruitmentDepartmentCommand> departments,
        List<StaffRecruitmentQuestionCommand> questions
) {
}
