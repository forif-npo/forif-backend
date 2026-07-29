package org.forif_backend.application.semester;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.semester.ActiveSemester;
import org.forif_backend.domain.semester.SemesterChangeLog;
import org.forif_backend.domain.semester.SemesterRepository;
import org.forif_backend.application.semester.dto.SemesterChangePreview;
import org.forif_backend.domain.hackathon.HackathonRepository;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 활동 학기 조회·변경.
 *
 * 학기는 시스템 날짜가 아니라 운영진이 지정한 값을 따른다.
 * 날짜 계산(DateUtils)은 설정 행이 아직 없을 때의 초기값으로만 쓰인다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterService {

    /** 학기 선택 목록의 시작 연도 (동아리 창립) */
    private static final int FIRST_YEAR = 2018;

    private final SemesterRepository semesterRepository;
    private final ForifTeamRepository forifTeamRepository;
    private final HackathonRepository hackathonRepository;

    /**
     * 현재 활동 학기.
     * 설정 행이 없으면 날짜 기준으로 계산한 값을 반환한다(저장하지는 않는다).
     */
    public SemesterInfo getActive() {
        return semesterRepository.findActive()
                .map(SemesterInfo::from)
                .orElseGet(() -> {
                    log.warn("활동 학기 설정이 없어 날짜 기준값을 사용합니다. 운영진 페이지에서 학기를 지정해주세요.");
                    return SemesterInfo.of(DateUtils.getCurrentYear(), DateUtils.getCurrentSemester());
                });
    }

    public int getActiveYear() {
        return getActive().actYear();
    }

    public int getActiveSemester() {
        return getActive().actSemester();
    }

    /** 선택 가능한 학기 목록 (창립 학기 ~ 현재 활동 학기의 다음 학기), 최신순 */
    public List<SemesterInfo> getSelectableSemesters() {
        SemesterInfo last = getActive().next();

        List<SemesterInfo> semesters = new ArrayList<>();
        for (int year = FIRST_YEAR; year <= last.actYear(); year++) {
            for (int semester = 1; semester <= 2; semester++) {
                if (year == last.actYear() && semester > last.actSemester()) break;
                semesters.add(SemesterInfo.of(year, semester));
            }
        }
        semesters.sort((a, b) -> a.actYear() != b.actYear()
                ? Integer.compare(b.actYear(), a.actYear())
                : Integer.compare(b.actSemester(), a.actSemester()));
        return semesters;
    }

    /**
     * 활동 학기 변경 (회장단 전용 — 권한 검증은 호출부에서 수행).
     */
    @Transactional
    public SemesterInfo changeActive(int actYear, int actSemester, Long changedBy) {
        validate(actYear, actSemester);

        // 설정 행이 없으면 폴백값이 곧 이전 학기다. 첫 전환도 이력에 남겨야 한다.
        SemesterInfo previous = getActive();
        if (previous.actYear() == actYear && previous.actSemester() == actSemester) {
            return previous;
        }

        ActiveSemester active = semesterRepository.findActive().orElse(null);
        SemesterInfo changed;
        if (active == null) {
            changed = SemesterInfo.from(semesterRepository.save(
                    ActiveSemester.create(actYear, actSemester, changedBy)));
        } else {
            active.change(actYear, actSemester, changedBy);
            changed = SemesterInfo.from(active);
        }

        semesterRepository.saveChangeLog(SemesterChangeLog.of(
                previous.actYear(), previous.actSemester(), actYear, actSemester, changedBy));

        log.info("활동 학기 변경: {} → {} (by {})", previous.label(), changed.label(), changedBy);
        return changed;
    }

    /** 전환 전 영향 미리보기 */
    public SemesterChangePreview preview(int targetYear, int targetSemester) {
        validate(targetYear, targetSemester);
        SemesterInfo current = getActive();

        return new SemesterChangePreview(
                current,
                SemesterInfo.of(targetYear, targetSemester),
                forifTeamRepository.findByYearAndSemester(targetYear, targetSemester).size(),
                forifTeamRepository.findByYearAndSemester(current.actYear(), current.actSemester()).size(),
                !hackathonRepository.findEvents(targetYear, targetSemester, null).isEmpty()
        );
    }

    /**
     * 학기 전환 + 부수 작업.
     * copyTeamMembers가 true면 현재 학기 운영진 명단을 대상 학기로 복제한다
     * (복제하지 않으면 운영진 소개 페이지가 빈다).
     */
    @Transactional
    public SemesterInfo changeActiveWithRollover(int actYear, int actSemester,
                                                 boolean copyTeamMembers, Long changedBy) {
        SemesterInfo previous = getActive();
        SemesterInfo changed = changeActive(actYear, actSemester, changedBy);

        if (copyTeamMembers && !previous.equals(changed)) {
            copyTeamMembers(previous, changed);
        }
        return changed;
    }

    /** 이전 학기 운영진 명단을 새 학기로 복제 (이미 있는 사람은 건너뜀) */
    private void copyTeamMembers(SemesterInfo from, SemesterInfo to) {
        List<ForifTeam> sources = forifTeamRepository.findByYearAndSemester(from.actYear(), from.actSemester());
        int copied = 0;

        for (ForifTeam source : sources) {
            Long userId = source.getUser() != null ? source.getUser().getId() : null;
            if (userId == null) continue;
            if (forifTeamRepository.existsByActYearAndActSemesterAndUserId(
                    to.actYear(), to.actSemester(), userId)) {
                continue;
            }

            ForifTeam copy = ForifTeam.create(source.getUser(), to.actYear(), to.actSemester(),
                    source.getClubDepartment());
            copy.update(source.getUserTitle(), source.getClubDepartment(), source.getIntroTag(),
                    source.getSelfIntro(), source.getProfImgUrl(), source.getGraduateYear());
            forifTeamRepository.save(copy);
            copied++;
        }
        log.info("운영진 명단 복제: {} → {} ({}명)", from.label(), to.label(), copied);
    }

    private void validate(int actYear, int actSemester) {
        if (actSemester != 1 && actSemester != 2) {
            throw new ForifException(ErrorCode.SEMESTER_INVALID);
        }
        // 창립 이전이나 현재 연도보다 지나치게 미래인 값은 오입력으로 본다
        int maxYear = DateUtils.getCurrentYear() + 1;
        if (actYear < FIRST_YEAR || actYear > maxYear) {
            throw new ForifException(ErrorCode.SEMESTER_INVALID);
        }
    }
}
