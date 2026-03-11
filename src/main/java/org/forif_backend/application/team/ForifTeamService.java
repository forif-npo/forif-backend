package org.forif_backend.application.team;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.web.team.dto.ForifTeamResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForifTeamService {

    private final ForifTeamRepository forifTeamRepository;

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
