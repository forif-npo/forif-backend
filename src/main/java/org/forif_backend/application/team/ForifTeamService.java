package org.forif_backend.application.team;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.user.UserService;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.team.dto.ForifTeamResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ForifTeamService {

    private final ForifTeamRepository forifTeamRepository;
    private final UserRepository userRepository;
    private final SemesterService semesterService;
    private final UserService userService;
    private final StaffAccountService staffAccountService;

    @Transactional(readOnly = true)
    public List<ForifTeamResponse> getAllMembers() {
        return forifTeamRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ForifTeamResponse> getMembersByYearAndSemester(int actYear, int actSemester) {
        return forifTeamRepository.findByYearAndSemester(actYear, actSemester).stream()
                .map(this::toResponse)
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
            forifTeam.update(userTitle, null, null, null, null);
        }
        return toResponse(forifTeamRepository.save(forifTeam));
    }

    @Transactional
    public ForifTeamResponse updateMember(Long requesterId, Long id, String userTitle, String clubDepartment,
                                          String introTag, String selfIntro, Integer graduateYear) {
        ForifTeam forifTeam = forifTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));

        if (staffAccountService.isPresidentTeam(requesterId)) {
            forifTeam.update(userTitle, clubDepartment, introTag, selfIntro, graduateYear);
        } else {
            validateOwner(requesterId, forifTeam);
            validateSelfUpdateFields(forifTeam, userTitle, clubDepartment);
            forifTeam.update(null, null, introTag, selfIntro, graduateYear);
        }

        return toResponse(forifTeam);
    }

    @Transactional
    public ForifTeamResponse updateMemberProfileImage(Long requesterId, Long id, MultipartFile profileImage) {
        ForifTeam forifTeam = forifTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));

        if (!staffAccountService.isPresidentTeam(requesterId)) {
            validateOwner(requesterId, forifTeam);
        }

        userService.updateUserProfileImage(forifTeam.getUser().getId(), profileImage);
        return toResponse(forifTeam);
    }

    @Transactional
    public void deleteMember(Long id) {
        forifTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));
        forifTeamRepository.deleteById(id);
    }

    private void validateOwner(Long requesterId, ForifTeam forifTeam) {
        if (!forifTeam.getUser().getId().equals(requesterId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
    }

    private void validateSelfUpdateFields(ForifTeam forifTeam, String userTitle, String clubDepartment) {
        if ((userTitle != null && !Objects.equals(userTitle, forifTeam.getUserTitle()))
                || (clubDepartment != null && !Objects.equals(clubDepartment, forifTeam.getClubDepartment()))) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
    }

    private ForifTeamResponse toResponse(ForifTeam forifTeam) {
        return ForifTeamResponse.from(
                forifTeam,
                userService.getProfileImageUrl(forifTeam.getUser().getImgUrl())
        );
    }
}
