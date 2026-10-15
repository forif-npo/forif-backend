package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffReviewerStatus;

/**
 * 열람자 초대 후보. 회차 학기의 운영진 명단을 기준으로 만들고 ADMIN 계정 보유 여부를 함께 알려준다.
 * 계정이 없는 사람은 목록에는 보이되 부여는 거절한다.
 */
public record StaffReviewerCandidateInfo(
        Long userId,
        String userName,
        String department,
        String affiliation,
        boolean hasAdminAccount,
        StaffReviewerStatus reviewerStatus
) {
}
