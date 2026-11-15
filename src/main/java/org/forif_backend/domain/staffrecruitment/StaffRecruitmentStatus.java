package org.forif_backend.domain.staffrecruitment;

/**
 * 운영진 모집 회차의 저장 상태.
 * 공개 응답의 접수 상태(RecruitmentApplicationStatus)와는 구분한다.
 */
public enum StaffRecruitmentStatus {
    DRAFT,      // 작성 중. 공개되지 않는다.
    PUBLISHED,  // 게시됨. 동시에 한 회차만 가능하다.
    CLOSED,     // 마감됨 (자연 종료 또는 조기 마감)
    CANCELED;   // 취소됨

    /** 종단 상태는 다른 상태로 전이할 수 없다. */
    public boolean isTerminal() {
        return this == CLOSED || this == CANCELED;
    }

    public boolean isPublished() {
        return this == PUBLISHED;
    }

    /** 공개 API에서 노출할 수 있는 상태인지. 초안은 노출하지 않는다. */
    public boolean isPubliclyVisible() {
        return this != DRAFT;
    }
}
