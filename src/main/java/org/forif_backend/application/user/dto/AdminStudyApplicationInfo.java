package org.forif_backend.application.user.dto;

import java.time.LocalDateTime;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;

/** 어드민 신청자 관리 화면의 한 행. 1·2순위 신청을 각각 별도 행으로 표현한다. */
public record AdminStudyApplicationInfo(
        Long applicationId,
        Long userId,
        String userName,
        String department,
        Integer studyId,
        String studyName,
        int priority,
        UserApplyStatus status,
        boolean autonomousStudy,
        LocalDateTime appliedAt
) {
    public static AdminStudyApplicationInfo primary(UserApply apply, Integer autonomousStudyId) {
        return of(apply, apply.getPrimaryStudy(), apply.getPrimaryStudyName(), 1,
                apply.getPrimaryStatus(), autonomousStudyId);
    }

    public static AdminStudyApplicationInfo secondary(UserApply apply, Integer autonomousStudyId) {
        return of(apply, apply.getSecondaryStudy(), apply.getSecondaryStudyName(), 2,
                apply.getSecondaryStatus(), autonomousStudyId);
    }

    private static AdminStudyApplicationInfo of(
            UserApply apply,
            Integer studyId,
            String studyName,
            int priority,
            UserApplyStatus status,
            Integer autonomousStudyId
    ) {
        return new AdminStudyApplicationInfo(
                apply.getId(), apply.getApplier().getId(), apply.getApplier().getUserName(),
                apply.getApplier().getDepartment(), studyId, studyName, priority, status,
                studyId.equals(autonomousStudyId), apply.getCreatedAt());
    }
}
