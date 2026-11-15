package org.forif_backend.domain.staffrecruitment;

/**
 * 회차별 열람 권한 상태. 저장하지 않고 회수 시각·만료 시각과 현재 시각으로 계산한다.
 */
public enum StaffReviewerStatus {
    ACTIVE,
    REVOKED,
    EXPIRED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
