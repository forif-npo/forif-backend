package org.forif_backend.domain.semester;

/**
 * 학기 일정 단계.
 *
 * 모집 단계는 시작·종료 시각을 가지며, 그 기간 안에서만 해당 기능이 동작한다.
 * 모집과 심사는 겹칠 수 없다 — 모집 창구가 열려 있는 동안 심사가 진행되면
 * 나중에 지원한 사람이 불리해지기 때문이다.
 */
public enum SemesterPhase {

    /** 멘토 모집 — 스터디 개설 신청 */
    MENTOR_RECRUIT("멘토 모집"),

    /** 멘토 수락/거절 — 운영진의 개설 승인·반려 */
    MENTOR_REVIEW("멘토 수락/거절"),

    /** 멘티 모집 — 수강 신청 */
    MENTEE_RECRUIT("멘티 모집"),

    /** 멘티 수락/거절 — 멘토의 합격·불합격 처리 */
    MENTEE_REVIEW("멘티 수락/거절"),

    /** 승인된 스터디를 실제 개설 상태로 일괄 전환하는 기준일 */
    STUDY_START("스터디 시작");

    private final String label;

    SemesterPhase(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 진행 순서. 앞 단계가 끝난 뒤에 뒤 단계가 시작되어야 한다. */
    public static SemesterPhase[] inOrder() {
        return new SemesterPhase[]{MENTOR_RECRUIT, MENTOR_REVIEW, MENTEE_RECRUIT, MENTEE_REVIEW, STUDY_START};
    }
}
