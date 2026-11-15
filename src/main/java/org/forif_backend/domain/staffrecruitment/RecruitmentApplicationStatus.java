package org.forif_backend.domain.staffrecruitment;

/**
 * 공개 응답에서 계산해 내려주는 접수 상태.
 * 저장하지 않고 저장 상태와 기간, 현재 시각으로 매 요청 계산한다.
 */
public enum RecruitmentApplicationStatus {
    UPCOMING,  // 게시됐지만 시작 전
    OPEN,      // 접수 중
    CLOSED,    // 마감
    CANCELED;  // 취소

    public boolean isOpen() {
        return this == OPEN;
    }
}
