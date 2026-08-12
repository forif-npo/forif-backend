package org.forif_backend.application.staff.dto;

import org.forif_backend.domain.user.User;

public record MentorSummary(
        Long userId,
        String name,
        String department,
        String phoneNum,
        String studyName,
        boolean manageable
) {
    public static MentorSummary from(User user, String studyName, boolean manageable) {
        return new MentorSummary(
                user.getId(),
                user.getUserName(),
                user.getDepartment(),
                user.getPhoneNum(),
                studyName,
                manageable
        );
    }
}
