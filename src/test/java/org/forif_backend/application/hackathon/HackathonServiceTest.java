package org.forif_backend.application.hackathon;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.domain.hackathon.HackathonEvent;
import org.forif_backend.domain.hackathon.HackathonRepository;
import org.forif_backend.domain.hackathon.HackathonStatus;
import org.forif_backend.domain.hackathon.CompetitionType;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HackathonServiceTest extends DefaultMockitoTest {

    @Autowired
    HackathonService hackathonService;

    @Autowired
    HackathonRepository hackathonRepository;

    @Test
    @DisplayName("운영진(ADMIN) 계정 사용자는 해커톤에 참가 등록할 수 있고 중복 등록은 실패한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void registerParticipantByStaffAccount() {
        Long hackathonId = createDefaultHackathon();

        ParticipantResponse response = hackathonService.registerParticipant(hackathonId, 1L);

        assertThat(response.status().name()).isEqualTo("REGISTERED");
        assertThatThrownBy(() -> hackathonService.registerParticipant(hackathonId, 1L))
                .hasMessage(ErrorCode.HACKATHON_ALREADY_REGISTERED.getMessage());
    }

    @Test
    @DisplayName("이번 학기 멘토는 study_user에 없어도 해커톤에 참가 등록할 수 있다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_study (study_id, act_year, act_semester, study_name, primary_mentor_id, primary_mentor_name, study_status, created_at, updated_at) VALUES (1001, 2025, 2, '웹 스터디', 1, '표준성', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_mentor_study (mentor_id, study_id, mentor_num) VALUES (1, 1001, 1)"
    })
    void registerParticipantByCurrentSemesterMentorStudy() {
        Long hackathonId = createDefaultHackathon();

        ParticipantResponse response = hackathonService.registerParticipant(hackathonId, 1L);

        assertThat(response.status().name()).isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("어드민 참가자 목록에서 참가자의 이번 학기 스터디를 함께 조회한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_study (study_id, act_year, act_semester, study_name, primary_mentor_id, primary_mentor_name, study_status, created_at, updated_at) VALUES (1001, 2025, 2, '웹 스터디', 1, '표준성', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_mentor_study (mentor_id, study_id, mentor_num) VALUES (1, 1001, 1)"
    })
    void getParticipantsIncludesCurrentSemesterStudies() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);

        ParticipantResponse participant = hackathonService.getParticipants(
                hackathonId,
                null,
                false
        ).get(0);

        assertThat(participant.studies()).hasSize(1);
        assertThat(participant.studies().get(0).studyName()).isEqualTo("웹 스터디");
        assertThat(participant.studies().get(0).role().name()).isEqualTo("MENTOR");
    }

    @Test
    @DisplayName("팀 생성자는 리더로 자동 등록되고 한 해커톤에서 두 팀에 들어갈 수 없다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void createTeamRegistersLeader() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);

        TeamResponse team = hackathonService.createTeam(
                hackathonId,
                1L,
                new CreateTeamRequest("팀 A", "주제", "소개", CompetitionType.IDEATHON, 4)
        );

        assertThat(team.competitionType()).isEqualTo(CompetitionType.IDEATHON);
        assertThat(team.members()).hasSize(1);
        assertThat(team.members().get(0).role().name()).isEqualTo("LEADER");
        assertThatThrownBy(() -> hackathonService.createTeam(
                hackathonId,
                1L,
                new CreateTeamRequest("팀 B", "주제", "소개", CompetitionType.HACKATHON, 4)
        )).hasMessage(ErrorCode.HACKATHON_ALREADY_TEAM_MEMBER.getMessage());
    }

    @Test
    @DisplayName("같은 대회에 아이디어톤과 해커톤 팀을 각각 생성할 수 있다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (2, 'pw', '양병현', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void createTeamsWithDifferentCompetitionTypesInSameEvent() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.registerParticipant(hackathonId, 2L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);

        TeamResponse ideathonTeam = hackathonService.createTeam(
                hackathonId, 1L, new CreateTeamRequest("아이디어팀", null, null, CompetitionType.IDEATHON, 4));
        TeamResponse hackathonTeam = hackathonService.createTeam(
                hackathonId, 2L, new CreateTeamRequest("개발팀", null, null, CompetitionType.HACKATHON, 4));

        assertThat(ideathonTeam.hackathonId()).isEqualTo(hackathonId);
        assertThat(hackathonTeam.hackathonId()).isEqualTo(hackathonId);
        assertThat(ideathonTeam.competitionType()).isEqualTo(CompetitionType.IDEATHON);
        assertThat(hackathonTeam.competitionType()).isEqualTo(CompetitionType.HACKATHON);
    }

    @Test
    @DisplayName("해커톤 진행 중에도 팀장은 팀 정보를 수정할 수 있다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void updateTeamDuringInProgress() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(
                hackathonId,
                1L,
                new CreateTeamRequest("팀 A", "기존 주제", "기존 소개", CompetitionType.HACKATHON, 4)
        );
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);

        TeamResponse updated = hackathonService.updateTeam(
                hackathonId,
                team.hackathonTeamId(),
                1L,
                new UpdateTeamRequest("팀 B", "새 주제", "새 소개", CompetitionType.HACKATHON, 5)
        );

        assertThat(updated.name()).isEqualTo("팀 B");
        assertThat(updated.topic()).isEqualTo("새 주제");
        assertThat(updated.description()).isEqualTo("새 소개");
        assertThat(updated.competitionType()).isEqualTo(CompetitionType.HACKATHON);
        assertThat(updated.maxMembers()).isEqualTo(5);
    }

    @Test
    @DisplayName("심사 단계에서는 팀 정보를 수정할 수 없다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void updateTeamDuringJudgingFails() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(
                hackathonId,
                1L,
                new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4)
        );
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.JUDGING);

        assertThatThrownBy(() -> hackathonService.updateTeam(
                hackathonId,
                team.hackathonTeamId(),
                1L,
                new UpdateTeamRequest("팀 B", null, null, null, 4)
        )).hasMessage(ErrorCode.HACKATHON_INVALID_STATUS.getMessage());
    }

    @Test
    @DisplayName("해커톤 생성 시 회차별 업로드 디렉터리를 준비한다")
    void createHackathonPreparesUploadDirectory() {
        createDefaultHackathon();

        verify(filePort).createDirectory("hackathons/2025-2");
    }

    @Test
    @DisplayName("한 학기에는 하나의 대회 회차만 만들 수 있다")
    void createHackathonRejectsAnotherRoundInTheSameSemester() {
        createDefaultHackathon();
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> hackathonService.createHackathon(new CreateHackathonRequest(
                2025,
                2,
                "FORIF 아이디어톤",
                "설명",
                "장소",
                now.minusDays(2),
                now.plusDays(1),
                now.plusDays(1),
                now.plusDays(2),
                now.plusDays(3),
                now.plusDays(4)
        ))).hasMessage(ErrorCode.HACKATHON_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("삭제한 해커톤과 같은 학기에 새 회차를 생성할 수 있다")
    void createHackathonAllowsRecreationAfterSoftDeletion() {
        Long deletedHackathonId = createDefaultHackathon();
        hackathonService.deleteHackathon(deletedHackathonId);
        LocalDateTime now = LocalDateTime.now();

        Long recreatedHackathonId = hackathonService.createHackathon(new CreateHackathonRequest(
                2025,
                2,
                "수정된 FORIF 해커톤",
                "설명",
                "장소",
                now.minusDays(2),
                now.plusDays(1),
                now.plusDays(1),
                now.plusDays(2),
                now.plusDays(3),
                now.plusDays(4)
        )).hackathonId();

        assertThat(recreatedHackathonId).isNotEqualTo(deletedHackathonId);
        assertThat(hackathonService.getHackathon(recreatedHackathonId).eventRound()).isEqualTo(2);
        assertThatThrownBy(() -> hackathonService.getHackathon(deletedHackathonId))
                .hasMessage(ErrorCode.HACKATHON_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("삭제된 해커톤 참가자는 수료 요건의 해커톤 참여자로 집계하지 않는다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void deletedHackathonParticipantsAreExcludedFromCertificateEligibility() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);

        assertThat(hackathonRepository.findRegisteredUserIdsBySemester(2025, 2)).containsExactly(1L);

        hackathonService.deleteHackathon(hackathonId);

        assertThat(hackathonRepository.findRegisteredUserIdsBySemester(2025, 2)).isEmpty();
    }

    @Test
    @DisplayName("새 대회에는 기존 최대 회차보다 1 큰 회차가 자동으로 부여된다")
    void createHackathonAssignsNextEventRound() {
        createDefaultHackathon();
        LocalDateTime now = LocalDateTime.now();

        Long hackathonId = hackathonService.createHackathon(new CreateHackathonRequest(
                2026,
                1,
                "FORIF 아이디어톤",
                "설명",
                "장소",
                now.minusDays(2),
                now.plusDays(1),
                now.plusDays(1),
                now.plusDays(2),
                now.plusDays(3),
                now.plusDays(4)
        )).hackathonId();

        assertThat(hackathonService.getHackathon(hackathonId).eventRound()).isEqualTo(2);
    }

    @Test
    @DisplayName("17회차 이벤트가 있으면 다음 대회에는 18회차가 자동 부여된다")
    void createHackathonAssignsEighteenthRoundAfterSeventeenthEvent() {
        LocalDateTime now = LocalDateTime.now();
        hackathonRepository.saveEvent(HackathonEvent.create(
                2025,
                2,
                17,
                "FORIF 해커톤",
                "설명",
                "장소",
                now.minusDays(2),
                now.plusDays(1),
                now.plusDays(1),
                now.plusDays(2),
                now.plusDays(3),
                now.plusDays(4)
        ));

        Long hackathonId = hackathonService.createHackathon(new CreateHackathonRequest(
                2026,
                1,
                "FORIF 아이디어톤",
                "설명",
                "장소",
                now.minusDays(2),
                now.plusDays(1),
                now.plusDays(1),
                now.plusDays(2),
                now.plusDays(3),
                now.plusDays(4)
        )).hackathonId();

        assertThat(hackathonService.getHackathon(hackathonId).eventRound()).isEqualTo(18);
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
    @DisplayName("팀 빌딩 시작 시간이 지나면 모집 상태가 자동으로 팀 빌딩으로 전환되고 참가 등록은 막힌다")
    void autoPromoteToTeamBuildingAfterRecruitmentEnds() {
        LocalDateTime now = LocalDateTime.now();
        Long hackathonId = hackathonService.createHackathon(new CreateHackathonRequest(
                2026,
                1,
                "자동 전환 해커톤",
                "설명",
                "장소",
                now.minusDays(2),
                now.minusHours(1),
                now.minusHours(1),
                now.plusDays(1),
                now.plusDays(2),
                now.plusDays(3)
        )).hackathonId();

        assertThat(hackathonService.getHackathon(hackathonId).status())
                .isEqualTo(HackathonStatus.TEAM_BUILDING);
        assertThatThrownBy(() -> hackathonService.registerParticipant(hackathonId, 1L))
                .hasMessage(ErrorCode.HACKATHON_REGISTRATION_CLOSED.getMessage());
    }

    @Test
    @DisplayName("해커톤 종료 시간이 지나면 자동으로 심사 상태로 전환된다")
    void autoPromoteToJudgingAfterHackathonEnds() {
        LocalDateTime now = LocalDateTime.now();
        Long hackathonId = hackathonService.createHackathon(new CreateHackathonRequest(
                2026,
                1,
                "심사 전환 해커톤",
                "설명",
                "장소",
                now.minusDays(4),
                now.minusDays(3),
                now.minusDays(3),
                now.minusDays(2),
                now.minusDays(2),
                now.minusDays(1)
        )).hackathonId();

        assertThat(hackathonService.getHackathon(hackathonId).status())
                .isEqualTo(HackathonStatus.JUDGING);
    }

    @Test
    @DisplayName("팀 목록은 해커톤 참가 등록자만 조회할 수 있다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (2, 'pw', '양병현', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void teamReadRequiresParticipant() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));

        assertThat(hackathonService.getTeamsForParticipant(hackathonId, 1L)).hasSize(1);
        assertThatThrownBy(() -> hackathonService.getTeamsForParticipant(hackathonId, 2L))
                .hasMessage(ErrorCode.HACKATHON_PARTICIPANT_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("팀 해산 시 대기 중인 가입 신청을 취소하고 팀원 레코드를 제거한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (2, 'pw', '양병현', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void disbandTeamCancelsPendingJoinRequests() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.registerParticipant(hackathonId, 2L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));
        hackathonService.createJoinRequest(hackathonId, team.hackathonTeamId(), 2L, new CreateJoinRequest("함께하고 싶습니다"));

        hackathonService.disbandTeam(hackathonId, team.hackathonTeamId(), 1L);

        assertThat(hackathonRepository.findJoinRequests(team.hackathonTeamId(), JoinRequestStatus.PENDING)).isEmpty();
        assertThat(hackathonRepository.findJoinRequests(team.hackathonTeamId(), JoinRequestStatus.CANCELED)).hasSize(1);
        assertThat(hackathonRepository.findTeamMembers(team.hackathonTeamId())).isEmpty();
        assertThat(hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 B", null, null, CompetitionType.HACKATHON, 4)).name())
                .isEqualTo("팀 B");
    }

    @Test
    @DisplayName("제출 수정 시 발표자료를 새로 첨부하지 않으면 기존 파일 경로를 유지한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void updateSubmissionKeepsExistingPresentationFile() {
        when(filePort.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn("hackathons/2025-2/presentation-v1.pdf");
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);
        SubmissionRequest createRequest = new SubmissionRequest(
                "프로젝트",
                "요약",
                "설명",
                "https://github.com/forif/example",
                null,
                null,
                List.of("React")
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
                        List.of("React")
                ),
                null
        );

        assertThat(updated.presentationFile()).isEqualTo("http://mock-file-url.com/hackathons/2025-2/presentation-v1.pdf");
    }

    @Test
    @DisplayName("제출 수정 시 발표자료를 새로 첨부하면 새 파일 경로로 교체한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void updateSubmissionReplacesPresentationFile() {
        when(filePort.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn("hackathons/2025-2/presentation-v1.pdf", "hackathons/2025-2/presentation-v2.pdf");
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);
        SubmissionRequest request = new SubmissionRequest(
                "프로젝트",
                "요약",
                "설명",
                "https://github.com/forif/example",
                null,
                null,
                List.of("React")
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

        assertThat(updated.presentationFile()).isEqualTo("http://mock-file-url.com/hackathons/2025-2/presentation-v2.pdf");
    }

    @Test
    @DisplayName("50자를 초과한 해커톤 기술 태그는 저장할 수 없다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void createSubmissionRejectsTooLongTechStack() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);

        SubmissionRequest request = new SubmissionRequest(
                "프로젝트",
                "요약",
                "설명",
                "https://github.com/forif/example",
                null,
                null,
                List.of("a".repeat(51))
        );

        assertThatThrownBy(() -> hackathonService.createSubmission(
                hackathonId,
                team.hackathonTeamId(),
                1L,
                request,
                null
        )).hasMessage(ErrorCode.HACKATHON_INVALID_TECH_STACK.getMessage());
    }

    @Test
    @DisplayName("기술 스택은 대소문자와 공백을 정규화하고 자유 입력을 저장한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void createSubmissionNormalizesKnownTechStacksAndAllowsCustomTechStack() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L,
                new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);

        SubmissionResponse response = hackathonService.createSubmission(
                hackathonId,
                team.hackathonTeamId(),
                1L,
                new SubmissionRequest(
                        "프로젝트",
                        "요약",
                        null,
                        null,
                        null,
                        null,
                        List.of(" react ", "REACT", "FastAPI")
                ),
                null
        );

        assertThat(response.techStacks()).containsExactly("React", "FastAPI");
    }

    @Test
    @DisplayName("공백 기술 스택은 무시하고 제출 결과물을 저장한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void createSubmissionIgnoresBlankTechStacks() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L,
                new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);

        SubmissionResponse response = hackathonService.createSubmission(
                hackathonId,
                team.hackathonTeamId(),
                1L,
                new SubmissionRequest("프로젝트", "요약", null, null, null, null, List.of(" ", "\t")),
                null
        );

        assertThat(response.techStacks()).isEmpty();
    }

    @Test
    @DisplayName("해커톤 기술 스택은 최대 4개까지만 저장할 수 있다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void createSubmissionRejectsTooManyTechStacks() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 1L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse team = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);

        SubmissionRequest request = new SubmissionRequest(
                "프로젝트",
                "요약",
                "설명",
                "https://github.com/forif/example",
                null,
                null,
                List.of("React", "Next.js", "TypeScript", "PostgreSQL", "Supabase")
        );

        assertThatThrownBy(() -> hackathonService.createSubmission(
                hackathonId,
                team.hackathonTeamId(),
                1L,
                request,
                null
        )).hasMessage(ErrorCode.HACKATHON_INVALID_TECH_STACK.getMessage());
    }

    @Test
    @DisplayName("아카이브 제출작은 주요 수상 결과물 순서로 먼저 조회된다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (1, 'pw', '표준성', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (2, 'pw', '양병현', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (3, 'pw', '김동현', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (4, 'pw', '송준우', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_staff_account (user_id, password, name, role, affiliation, created_at, updated_at) VALUES (5, 'pw', '이서준', 'ADMIN', '운영진', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void getArchiveSubmissionsPrioritizesMajorAwards() {
        Long hackathonId = createDefaultHackathon();
        for (long userId = 1; userId <= 5; userId++) {
            hackathonService.registerParticipant(hackathonId, userId);
        }
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);
        TeamResponse normalTeam = hackathonService.createTeam(hackathonId, 1L, new CreateTeamRequest("일반 팀", null, null, CompetitionType.HACKATHON, 4));
        TeamResponse excellenceTeam = hackathonService.createTeam(hackathonId, 2L, new CreateTeamRequest("우수상 팀", null, null, CompetitionType.HACKATHON, 4));
        TeamResponse grandPrizeTeam = hackathonService.createTeam(hackathonId, 3L, new CreateTeamRequest("대상 팀", null, null, CompetitionType.HACKATHON, 4));
        TeamResponse ideathonTeam = hackathonService.createTeam(hackathonId, 4L, new CreateTeamRequest("아이디어톤 팀", null, null, CompetitionType.IDEATHON, 4));
        TeamResponse topExcellenceTeam = hackathonService.createTeam(hackathonId, 5L, new CreateTeamRequest("최우수상 팀", null, null, CompetitionType.HACKATHON, 4));

        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);
        createSubmission(hackathonId, normalTeam, 1L, "일반 프로젝트");
        createSubmission(hackathonId, excellenceTeam, 2L, "우수상 프로젝트");
        createSubmission(hackathonId, grandPrizeTeam, 3L, "대상 프로젝트");
        createSubmission(hackathonId, ideathonTeam, 4L, "아이디어톤 프로젝트");
        createSubmission(hackathonId, topExcellenceTeam, 5L, "최우수상 프로젝트");

        hackathonService.createAward(hackathonId, new AwardRequest(excellenceTeam.hackathonTeamId(), "우수상", 3));
        hackathonService.createAward(hackathonId, new AwardRequest(grandPrizeTeam.hackathonTeamId(), "대상", 1));
        hackathonService.createAward(hackathonId, new AwardRequest(ideathonTeam.hackathonTeamId(), "아이디어톤 특별상", null));
        hackathonService.createAward(hackathonId, new AwardRequest(topExcellenceTeam.hackathonTeamId(), "최우수상", 2));
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.JUDGING);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.ENDED);

        List<String> projectNames = hackathonService.getArchiveSubmissions(hackathonId, null, null).stream()
                .map(SubmissionResponse::projectName)
                .toList();

        assertThat(projectNames).containsExactly(
                "대상 프로젝트",
                "최우수상 프로젝트",
                "우수상 프로젝트",
                "아이디어톤 프로젝트",
                "일반 프로젝트"
        );
    }

    @Test
    @DisplayName("참가자는 본인 팀을 평가할 수 없고 다른 팀은 1~5점 객관식으로 평가한다")
    @Sql({"/sql/user-test-data.sql"})
    @Sql(statements = {
            // 운영진이 아니라 해당 학기 수강생으로 참가 자격을 얻는다.
            // 운영진(ADMIN) 계정이 있으면 평가자 유형이 ADMIN이 되어
            // 본인 팀 평가 금지 규칙을 검증할 수 없다.
            "INSERT INTO tb_study (study_id, act_year, act_semester, study_name, primary_mentor_id, primary_mentor_name, study_status, created_at, updated_at) VALUES (2001, 2025, 2, '평가 테스트 스터디', 1, '표준성', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO tb_study_user (study_id, user_id) VALUES (2001, 2)",
            "INSERT INTO tb_study_user (study_id, user_id) VALUES (2001, 3)"
    })
    void participantEvaluationRules() {
        Long hackathonId = createDefaultHackathon();
        hackathonService.registerParticipant(hackathonId, 2L);
        hackathonService.registerParticipant(hackathonId, 3L);
        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.TEAM_BUILDING);

        TeamResponse teamA = hackathonService.createTeam(hackathonId, 2L, new CreateTeamRequest("팀 A", null, null, CompetitionType.HACKATHON, 4));
        TeamResponse teamB = hackathonService.createTeam(hackathonId, 3L, new CreateTeamRequest("팀 B", null, null, CompetitionType.HACKATHON, 4));

        hackathonService.changeHackathonStatus(hackathonId, HackathonStatus.IN_PROGRESS);
        SubmissionRequest submissionRequest = new SubmissionRequest(
                "프로젝트",
                "요약",
                "설명",
                "https://github.com/forif/example",
                null,
                null,
                List.of("React")
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

    private SubmissionResponse createSubmission(Long hackathonId, TeamResponse team, Long userId, String projectName) {
        return hackathonService.createSubmission(
                hackathonId,
                team.hackathonTeamId(),
                userId,
                new SubmissionRequest(
                        projectName,
                        "요약",
                        "설명",
                        "https://github.com/forif/example",
                        null,
                        null,
                        List.of("React")
                ),
                null
        );
    }

    private Long createDefaultHackathon() {
        LocalDateTime now = LocalDateTime.now();
        return hackathonService.createHackathon(new CreateHackathonRequest(
                2025,
                2,
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
