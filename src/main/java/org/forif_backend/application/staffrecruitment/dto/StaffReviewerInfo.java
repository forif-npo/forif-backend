package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffRecruitmentReviewer;
import org.forif_backend.domain.staffrecruitment.StaffReviewerStatus;
import org.forif_backend.domain.user.User;

import java.time.LocalDateTime;

/**
 * 회차별 열람 권한 조회 모델. 상태는 저장값이 아니라 조회 시각으로 계산한다.
 */
public record StaffReviewerInfo(
        Long userId,
        String userName,
        String department,
        String affiliation,
        StaffReviewerStatus status,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt
) {

    public static StaffReviewerInfo from(StaffRecruitmentReviewer reviewer, String affiliation, LocalDateTime now) {
        User user = reviewer.getUser();
        return new StaffReviewerInfo(
                user.getId(),
                user.getUserName(),
                departmentNameOf(user),
                affiliation,
                reviewer.status(now),
                reviewer.getGrantedAt(),
                reviewer.getExpiresAt(),
                reviewer.getRevokedAt());
    }

    static String departmentNameOf(User user) {
        if (user.getDepartmentEntity() != null) {
            return user.getDepartmentEntity().getDepartmentName();
        }
        return user.getDepartment();
    }
}
