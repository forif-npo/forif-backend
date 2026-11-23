package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.dto.StaffRegistrationResult;
import org.forif_backend.application.team.ForifTeamService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.*;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 합격자를 차기 운영진 명단에 한 번에 올린다.
 *
 * 합격 처리는 면접 보드·지원서 보기에서 바로 하고, 명단 등록은 발표 시점에 맞춰 회장단이
 * 따로 누른다. 명단은 사이트 운영진 소개에 쓰이기 때문이다.
 * 등록 학기는 모집 회차의 활동 학기이고, 부서는 합격 때 배정한 부서다.
 * 실제 추가는 운영진 관리 서비스를 사용한다. 차기 학기 SW 합격자는 확정된 결과에 한해 회장단이 등록할 수 있다.
 * 직급은 팀원으로 넣는다. 차기 회장·부회장·팀장은 회장단이 운영진 목록의 차기 탭에서 따로 지정한다.
 */
@Service
@RequiredArgsConstructor
public class StaffInterviewStaffRegistrationService {

    private final StaffRecruitmentRepository staffRecruitmentRepository;
    private final StaffInterviewRecordRepository staffInterviewRecordRepository;
    private final ForifTeamRepository forifTeamRepository;
    private final ForifTeamService forifTeamService;

    /**
     * 합격자 중 아직 명단에 없는 사람만 올린다. 여러 번 눌러도 같은 사람이 두 번 오르지 않는다.
     * 탈퇴해 회원 정보가 없는 합격자는 올릴 수 없어 따로 돌려준다.
     */
    @Transactional
    public StaffRegistrationResult registerPassedApplicants(Long requesterId, Integer recruitmentId) {
        StaffRecruitment recruitment = staffRecruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
        if (recruitment.getStatus() != StaffRecruitmentStatus.CLOSED) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        }
        if (recruitment.getInterviewResultsFinalizedAt() == null) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_RESULTS_NOT_FINALIZED);
        }

        List<StaffInterviewRecord> passed = staffInterviewRecordRepository.findRecordsByRecruitmentId(recruitmentId)
                .stream()
                .filter(record -> record.getStatus() == StaffInterviewStatus.PASSED)
                .sorted(Comparator.comparing(record -> record.getApplication().getApplicantName()))
                .toList();

        List<String> registered = new ArrayList<>();
        List<String> alreadyMembers = new ArrayList<>();
        List<String> withdrawn = new ArrayList<>();
        int year = recruitment.getActYear();
        int semester = recruitment.getActSemester();

        for (StaffInterviewRecord record : passed) {
            StaffApplication application = record.getApplication();
            User applicant = application.getApplicant();
            if (applicant == null) {
                withdrawn.add(application.getApplicantName());
                continue;
            }
            if (forifTeamRepository.existsByActYearAndActSemesterAndUserId(year, semester, applicant.getId())) {
                alreadyMembers.add(application.getApplicantName());
                continue;
            }
            forifTeamService.addFinalizedRecruitmentMember(requesterId, applicant.getId(), year, semester,
                    clubDepartmentName(record.getPassedDepartment()));
            registered.add(application.getApplicantName());
        }
        return new StaffRegistrationResult(year, semester, registered, alreadyMembers, withdrawn);
    }

    /** 모집 부서가 동아리 부서와 이어져 있으면 그 이름을, 아니면 모집 부서 이름을 쓴다. */
    private String clubDepartmentName(StaffRecruitmentDepartment department) {
        if (department == null) {
            return null;
        }
        return department.getClubDepartment() != null
                ? department.getClubDepartment().getName()
                : department.getName();
    }
}
