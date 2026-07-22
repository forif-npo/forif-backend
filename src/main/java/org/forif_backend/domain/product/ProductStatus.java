package org.forif_backend.domain.product;

/**
 * 프로덕트 상태.
 * PENDING/REJECTED 는 등록 신청 단계, 나머지는 승인 후 게시 상태를 나타낸다.
 */
public enum ProductStatus {
    PENDING,    // 검토 대기
    REJECTED,   // 반려
    LIVE,       // 서비스 중
    DEV,        // 개발 중
    PAUSED,     // 운영 중단
    RETIRED;    // 서비스 종료

    public boolean isPublished() {
        return this == LIVE || this == DEV || this == PAUSED || this == RETIRED;
    }
}
