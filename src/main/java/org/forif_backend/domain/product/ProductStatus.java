package org.forif_backend.domain.product;

/**
 * 서비스 등록 신청 상태.
 */
public enum ProductStatus {
    PENDING,    // 검토 대기
    ACCEPTED,   // 승인
    REJECTED;   // 반려

    public boolean isAccepted() {
        return this == ACCEPTED;
    }
}
