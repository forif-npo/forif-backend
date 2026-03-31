package org.forif_backend.web.user.dto;

import lombok.Builder;

@Builder
public record MemberResponse(
        Long userId,
        String department,
        String userName,
        String phoneNum,
        String currentStudyName,
        boolean isMentor,
        boolean isAdmin
) {
}
