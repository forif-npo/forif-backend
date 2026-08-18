package org.forif_backend.application.semester.dto;

import org.forif_backend.domain.semester.ActiveSemester;

/**
 * 활동 학기 값 객체. label은 "26-1" 형태의 표기용 문자열이다.
 */
public record SemesterInfo(int actYear, int actSemester, String label) {

    public static SemesterInfo of(int actYear, int actSemester) {
        return new SemesterInfo(actYear, actSemester, toLabel(actYear, actSemester));
    }

    public static SemesterInfo from(ActiveSemester activeSemester) {
        return of(activeSemester.getActYear(), activeSemester.getActSemester());
    }

    public static String toLabel(int actYear, int actSemester) {
        return "%02d-%d".formatted(actYear % 100, actSemester);
    }

    /** 다음 학기 (2학기 다음은 이듬해 1학기) */
    public SemesterInfo next() {
        return actSemester == 1 ? of(actYear, 2) : of(actYear + 1, 1);
    }

}
