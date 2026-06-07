package org.forif_backend.application.hackathon;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.domain.hackathon.HackathonRepository;
import org.forif_backend.domain.hackathon.HackathonStatus;
import org.forif_backend.domain.hackathon.JoinRequestStatus;
import org.forif_backend.mock.DefaultMockitoTest;
import org.forif_backend.web.hackathon.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class HackathonServiceTest extends DefaultMockitoTest {

    @Autowired
    HackathonService hackathonService;

    @Autowired
    HackathonRepository hackathonRepository;

    @Test
    @DisplayName("staff_account 사용자는 해커톤에 참가 등록할 수 있고 중복 등록은 실패한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void registerParticipantByStaffAccount() {
        Long hackathonId = createDefaultHackathon();

        ParticipantResponse response = hackathonService.registerParticipant(hackathonId, 1L);

        assertThat(response.status().name()).isEqualTo("REGISTERED");
        assertThatThrownBy(() -> hackathonService.registerParticipant(hackathonId, 1L))
                .hasMessage(ErrorCode.HACKATHON_ALREADY_REGISTERED.getMessage());
    }

    @Test
    @DisplayName("팀 생성자는 리더로 자동 등록되고 한 해커톤에서 두 팀에 들어갈 수 없다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void createTeamRegistersLeader() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);

        TeamResponse team = hackathonService.createTeam(
                hackathonId,
                1L,
                new CreateTeamRequest("팀 A", "주제", "소개", 4)
        );

        assertThat(team.members()).hasSize(1);
        assertThat(team.members().get(0).role().name()).isEqualTo("LEADER");
        assertThatThrownBy(() -> hackathonService.createTeam(
                hackathonId,
                1L,
                new CreateTeamRequest("팀 B", "주제", "소개", 4)
        )).hasMessage(ErrorCode.HACKATHON_ALREADY_TEAM_MEMBER.getMessage());
    }

    @Test
    @DisplayName("해커톤 상태는 정해진 순서로만 전환할 수 있다")
    void changeStatusRequiresNextFlow() {
        Long hackathonId = createDefaultHackathon();

        assertThatThrownBy(() -> hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS))
                .hasMessage(ErrorCode.HACKATHON_INVALID_STATUS.getMessage());

        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);

        assertThatThrownBy(() -> hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.RECRUITING))
                .hasMessage(ErrorCode.HACKATHON_INVALID_STATUS.getMessage());
    }

    @Test
    @DisplayName("팀 목록은 해커톤 참가 등록자만 조회할 수 있다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (2, 'pw', '양병현', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void teamReadRequiresParticipant() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, 4));

        assertThat(hackathonService.getTeamsForParticipant(hackathonId, 1L)).hasSize(1);
        assertThatThrownBy(() -> hackathonService.getTeamsForParticipant(hackathonId, 2L))
                .hasMessage(ErrorCode.HACKATHON_PARTICIPANT_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("팀 해산 시 대기 중인 가입 신청을 취소하고 팀원 레코드를 제거한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (2, 'pw', '양병현', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void disbandTeamCancelsPendingJoinRequests() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.registerParticipant(hackathonId, 2L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, 4));
        hackathonService.createJoinRequest(hackathonId, team.hackathonTeamId(), 2L, new CreateJoinRequest("함께하고 싶습니다"));

        hackathonService.disbandTeam(hackathonId, team.hackathonTeamId(), 1L);

        assertThat(hackathonRepository.findJoinRequests(team.hackathonTeamId(), JoinRequestStatus.PENDING)).isEmpty();
        assertThat(hackathonRepository.findJoinRequests(team.hackathonTeamId(), JoinRequestStatus.CANCELED)).hasSize(1);
        assertThat(hackathonRepository.findTeamMembers(team.hackathonTeamId())).isEmpty();
        assertThat(hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 B", null, null, 4)).name())
                .isEqualTo("팀 B");
    }

    @Test
    @DisplayName("제출 수정 시 발표자료를 새로 첨부하지 않으면 기존 파일 경로를 유지한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void updateSubmissionKeepsExistingPresentationFile() {
        when(filePort.uploadFile(any(MultipartFile.class))).thenReturn("presentation-v1.pdf");
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, 4));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);
        SubmissionRequest createRequest = new SubmissionRequest(
                "프로젝트",
                "요약",
                "설명",
                "https://github.com/forif/example",
                null,
                null,
                List.of("Spring Boot")
        );
        MockMultipartFile presentation = new MockMultipartFile(
                "presentation",
                "presentation.pdf",
                "application/pdf",
                "pdf".getBytes()
        );
        hackathonService.createSubmission(hackathonId, team.hackathonTeamId(), 1L, createRequest, presentation);

        SubmissionResponse updated = hackathonService.updateSubmission(
                hackathonId,
                team.hackathonTeamId(),
                1L,
                new SubmissionRequest(
                        "프로젝트 수정",
                        "요약 수정",
                        "설명 수정",
                        "https://github.com/forif/example-updated",
                        null,
                        null,
                        List.of("Spring Boot")
                ),
                null
        );

        assertThat(updated.presentationFile()).isEqualTo("http://mock-file-url.com/presentation-v1.pdf");
    }

    @Test
    @DisplayName("제출 수정 시 발표자료를 새로 첨부하면 새 파일 경로로 교체한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void updateSubmissionReplacesPresentationFile() {
        when(filePort.uploadFile(any(MultipartFile.class))).thenReturn("presentation-v1.pdf", "presentation-v2.pdf");
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, 4));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);
        SubmissionRequest request = new SubmissionRequest(
                "프로젝트",
                "요약",
                "설명",
                "https://github.com/forif/example",
                null,
                null,
                List.of("Spring Boot")
        );
        MockMultipartFile firstPresentation = new MockMultipartFile(
                "presentation",
                "presentation-v1.pdf",
                "application/pdf",
                "pdf-v1".getBytes()
        );
        MockMultipartFile secondPresentation = new MockMultipartFile(
                "presentation",
                "presentation-v2.pdf",
                "application/pdf",
                "pdf-v2".getBytes()
        );
        hackathonService.createSubmission(hackathonId, team.hackathonTeamId(), 1L, request, firstPresentation);

        SubmissionResponse updated = hackathonService.updateSubmission(
                hackathonId,
                team.hackathonTeamId(),
                1L,
                request,
                secondPresentation
        );

        assertThat(updated.presentationFile()).isEqualTo("http://mock-file-url.com/presentation-v2.pdf");
    }

    @Test
    @DisplayName("참가자는 본인 팀을 평가할 수 없고 다른 팀은 1~5점 객관식으로 평가한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (2, 'pw', '양병현', 'MENTOR', '웹', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (3, 'pw', '김동현', 'MENTOR', '백엔드', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void participantEvaluationRules() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 2L);
        hackathonService.registerParticipant(hackathonId, 3L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);

        TeamResponse teamA = hackathonService.createTeam(hackathonId, 2L, new CreateTeamRequest("팀 A", null, null, 4));
        TeamResponse teamB = hackathonService.createTeam(hackathonId, 3L, new CreateTeamRequest("팀 B", null, null, 4));

        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);
        SubmissionRequest submissionRequest = new SubmissionRequest(
                "프로젝트",
                "요약",
                "설명",
                "https://github.com/forif/example",
                null,
                null,
                List.of("Spring Boot")
        );
        hackathonService.createSubmission(hackathonId, teamA.hackathonTeamId(), 2L, submissionRequest, null);
        hackathonService.createSubmission(hackathonId, teamB.hackathonTeamId(), 3L, submissionRequest, null);

        hackathonService.createCriterion(hackathonId, new CriterionRequest("창의성", null, 5, BigDecimal.ONE, 1));
        hackathonService.createCriterion(hackathonId, new CriterionRequest("완성도", null, 5, BigDecimal.ONE, 2));
        List<CriterionResponse> criteria = hackathonService.getCriteria(hackathonId);
        EvaluationRequest evaluationRequest = new EvaluationRequest(List.of(
                new EvaluationRequest.Score(criteria.get(0).criterionId(), 5),
                new EvaluationRequest.Score(criteria.get(1).criterionId(), 4)
        ));

        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.JUDGING);

        assertThatThrownBy(() -> hackathonService.createEvaluation(
                hackathonId, teamA.hackathonTeamId(), 2L, evaluationRequest
        )).hasMessage(ErrorCode.HACKATHON_SELF_EVALUATION_NOT_ALLOWED.getMessage());

        EvaluationResponse evaluation = hackathonService.createEvaluation(
                hackathonId, teamB.hackathonTeamId(), 2L, evaluationRequest
        );

        assertThat(evaluation.totalScore()).isEqualByComparingTo(BigDecimal.valueOf(9));
        assertThat(evaluation.scores()).hasSize(2);
    }

    private Long createDefaultHackathon() {
        LocalDateTime now = LocalDateTime.now();
        return hackathonService.createHackathon(new CreateHackathonRequest(
                2025,
                2,
                1,
                "FORIF 해커톤",
                "설명",
                "장소",
                now.minusDays(2),
                now.plusDays(1),
                now.plusDays(1),
                now.plusDays(2),
                now.plusDays(3),
                now.plusDays(4)
        )).hackathonId();
    }
}
