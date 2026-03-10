package org.forif_backend.application.team;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForifTeamService {

    private final ForifTeamRepository forIfTeamRepository;

    @Transactional(readOnly = true)
    public List<ForifTeam> getAllMembers() {
        return forIfTeamRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ForifTeam> getMembersByYearAndSemester(int actYear, int actSemester) {
        return forIfTeamRepository.findByYearAndSemester(actYear, actSemester);
    }

    @Transactional
    public ForifTeam updateMember(Long id, String userTitle, String clubDepartment,
                                  String introTag, String selfIntro, String profImgUrl, Integer graduateYear) {
        ForifTeam forIfTeam = forIfTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));
        forIfTeam.update(userTitle, clubDepartment, introTag, selfIntro, profImgUrl, graduateYear);
        return forIfTeam;
    }

    @Transactional
    public void deleteMember(Long id) {
        forIfTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));
        forIfTeamRepository.deleteById(id);
    }
}
