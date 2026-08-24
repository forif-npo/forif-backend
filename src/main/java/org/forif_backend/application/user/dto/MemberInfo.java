package org.forif_backend.application.user.dto;

import lombok.Builder;

/**
 * 부원 목록 항목. 특정 학기 기준의 수강 스터디명과 역할을 함께 담는다.
 */
@Builder
public record MemberInfo(
        Long userId,
        String department,
        String userName,
        String phoneNum,
        String currentStudyName,
        boolean isMentor,
        boolean isAdmin
) {
}
