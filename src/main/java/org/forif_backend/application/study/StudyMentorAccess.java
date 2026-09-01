package org.forif_backend.application.study;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.study.Study;
import org.springframework.stereotype.Component;

/**
 * 스터디에 대한 멘토 접근 권한.
 *
 * 멘토는 계정 종류가 아니라 "이 스터디의 멘토인가"라는 관계다. 그래서 별도 멘토
 * 계정 없이 부원 로그인만으로 자기 스터디를 관리할 수 있고, 스터디 멘토를 바꾸면
 * 다음 요청부터 즉시 반영된다.
 *
 * 읽기는 지난 학기 스터디도 허용한다. 자기가 운영했던 스터디의 지원자 명단을
 * 다시 볼 수 있어야 하기 때문이다. 쓰기는 활동 학기로 제한한다. 학기가 끝난 뒤에
 * 합불이나 출석이 바뀌면 이미 확정된 결과가 뒤집히기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class StudyMentorAccess {

    private final SemesterService semesterService;

    /** 조회용. 학기와 무관하게 본인이 멘토인 스터디면 통과한다. */
    public void requireMentor(Study study, Long userId) {
        if (!study.isMentor(userId)) {
            throw new ForifException(ErrorCode.NOT_STUDY_MENTOR);
        }
    }

    /** 변경용. 본인이 멘토이면서 활동 학기의 스터디여야 통과한다. */
    public void requireMentorOfActiveSemester(Study study, Long userId) {
        requireMentor(study, userId);

        SemesterInfo active = semesterService.getActive();
        if (!active.matches(study.getActYear(), study.getActSemester())) {
            throw new ForifException(ErrorCode.STUDY_NOT_IN_ACTIVE_SEMESTER);
        }
    }
}
