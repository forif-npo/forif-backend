package org.forif_backend.application.semester;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학기 전환 오케스트레이션.
 *
 * 학기 변경과 회장 인수인계를 한 트랜잭션으로 묶는다.
 * SemesterService가 StaffAccountService를 직접 참조하면 순환 의존이 되므로
 * (StaffAccountService도 활동 학기를 조회한다) 상위에서 조합한다.
 */
@Service
@RequiredArgsConstructor
public class SemesterTransitionService {

    private final SemesterService semesterService;
    private final StaffAccountService staffAccountService;

    /**
     * 학기 전환 + 차기 회장 인수인계.
     * 차기 회장은 필수이며 ADMIN 계정을 가진 기존 운영진이어야 한다.
     * 현 회장 본인을 지정하면 연임으로 처리된다.
     */
    @Transactional
    public SemesterInfo transition(int actYear, int actSemester,
                                   Long nextPresidentUserId, Long requesterId) {
        if (nextPresidentUserId == null || !staffAccountService.isAdminAccount(nextPresidentUserId)) {
            throw new ForifException(ErrorCode.SEMESTER_NEXT_PRESIDENT_REQUIRED);
        }

        SemesterInfo changed = semesterService.changeActive(actYear, actSemester, requesterId);
        staffAccountService.handOverPresidency(requesterId, nextPresidentUserId);
        return changed;
    }
}
