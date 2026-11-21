package org.forif_backend.domain.staffrecruitment;

/**
 * 면접 진행 상태. 보드의 네 칸과 같다.
 * 회장단이 보드에서 끌어 옮기므로 상태 사이의 이동을 막지 않는다.
 */
public enum StaffInterviewStatus {
    BEFORE,
    DONE,
    PASSED,
    FAILED
}
