package org.forif_backend.application.team;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.team.dto.ForifTeamResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForifTeamService {

    private final ForifTeamRepository forifTeamRepository;
    private final UserRepository userRepository;
    private final SemesterService semesterService;

    @Transactional(readOnly = true)
    public List<ForifTeamResponse> getAllMembers() {
        return forifTeamRepository.findAll().stream()
                .map(ForifTeamResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ForifTeamResponse> getMembersByYearAndSemester(int actYear, int actSemester) {
        return forifTeamRepository.findByYearAndSemester(actYear, actSemester).stream()
                .map(ForifTeamResponse::from)
                .toList();
    }

    /**
     * 운영진 명단에 추가 (회장단 전용 — 권한 검증은 호출부에서 수행).
     * 학기를 지정하지 않으면 현재 활동 학기에 추가한다.
     */
    @Transactional
    public ForifTeamResponse addMember(Long userId, Integer actYear, Integer actSemester,
                                       String clubDepartment, String userTitle) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        SemesterInfo active = semesterService.getActive();
        int year = actYear != null ? actYear : active.actYear();
        int semester = actSemester != null ? actSemester : active.actSemester();

        if (forifTeamRepository.existsByActYearAndActSemesterAndUserId(year, semester, userId)) {
            throw new ForifException(ErrorCode.FORIF_TEAM_MEMBER_ALREADY_EXISTS);
        }

        ForifTeam forifTeam = ForifTeam.create(user, year, semester, clubDepartment);
        if (userTitle != null && !userTitle.isBlank()) {
            forifTeam.update(userTitle, null, null, null, null, null);
        }
        return ForifTeamResponse.from(forifTeamRepository.save(forifTeam));
    }

    @Transactional
    public ForifTeamResponse updateMember(Long id, String userTitle, String clubDepartment,
                                          String introTag, String selfIntro, String profImgUrl, Integer graduateYear) {
        ForifTeam forifTeam = forifTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));
        forifTeam.update(userTitle, clubDepartment, introTag, selfIntro, profImgUrl, graduateYear);
        return ForifTeamResponse.from(forifTeam);
    }

    @Transactional
    public void deleteMember(Long id) {
        forifTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));
        forifTeamRepository.deleteById(id);
    }
}
