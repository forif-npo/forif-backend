package org.forif_backend.web.study.dto;

import java.util.List;
import org.forif_backend.application.study.dto.MentorConfirmationTargetsResult;

public record MentorConfirmationTargetsResponse(
        Integer studyId,
        String studyName,
        int actYear,
        int actSemester,
        List<Target> targets
) {
    public record Target(
            Long userId,
            String userName,
            String department,
            int confirmationStatus
    ) {
    }

    public static MentorConfirmationTargetsResponse from(MentorConfirmationTargetsResult result) {
        return new MentorConfirmationTargetsResponse(
                result.studyId(), result.studyName(), result.actYear(), result.actSemester(),
                result.targets().stream()
                        .map(target -> new Target(
                                target.userId(), target.userName(), target.department(),
                                target.confirmationStatus()))
                        .toList()
        );
    }
}
