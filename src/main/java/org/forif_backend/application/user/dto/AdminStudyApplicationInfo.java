package org.forif_backend.application.user.dto;

import java.time.LocalDateTime;
import org.forif_backend.domain.user.UserApply;

/** 어드민 신청자 관리 화면의 한 행. 1·2순위 신청을 각각 별도 행으로 표현한다. */
public record AdminStudyApplicationInfo(
        Long userId,
        String userName,
        String department,
        String studyName,
        int priority,
        LocalDateTime appliedAt
) {
    public static AdminStudyApplicationInfo primary(UserApply apply) {
        return of(apply, apply.getPrimaryStudyName(), 1);
    }

    public static AdminStudyApplicationInfo secondary(UserApply apply) {
        return of(apply, apply.getSecondaryStudyName(), 2);
    }

    private static AdminStudyApplicationInfo of(UserApply apply, String studyName, int priority) {
        return new AdminStudyApplicationInfo(
                apply.getApplier().getId(), apply.getApplier().getUserName(),
                apply.getApplier().getDepartment(), studyName, priority, apply.getCreatedAt());
    }
}
