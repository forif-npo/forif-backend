package org.forif_backend.infrastructure.persistence.hackathon;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.hackathon.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HackathonRepositoryImpl implements HackathonRepository {

    private final HackathonEventJpaRepository eventJpaRepository;
    private final HackathonParticipantJpaRepository participantJpaRepository;
    private final HackathonTeamJpaRepository teamJpaRepository;
    private final HackathonTeamMemberJpaRepository teamMemberJpaRepository;
    private final HackathonJoinRequestJpaRepository joinRequestJpaRepository;
    private final HackathonSubmissionJpaRepository submissionJpaRepository;
    private final HackathonSubmissionTechStackJpaRepository techStackJpaRepository;
    private final HackathonEvaluationCriterionJpaRepository criterionJpaRepository;
    private final HackathonEvaluationJpaRepository evaluationJpaRepository;
    private final HackathonEvaluationScoreJpaRepository evaluationScoreJpaRepository;
    private final HackathonAwardJpaRepository awardJpaRepository;

    @Override
    public HackathonEvent saveEvent(HackathonEvent event) {
        return eventJpaRepository.save(event);
    }

    @Override
    public Optional<HackathonEvent> findEventById(Long hackathonId) {
        return eventJpaRepository.findById(hackathonId)
                .filter(event -> event.getDeletedAt() == null);
    }

    @Override
    public List<HackathonEvent> findEvents(Integer year, Integer semester, HackathonStatus status) {
        return eventJpaRepository.search(year, semester, status);
    }

    @Override
    public boolean existsActiveEvent() {
        return eventJpaRepository.existsByDeletedAtIsNullAndStatusNot(HackathonStatus.ENDED);
    }

    @Override
    public boolean existsEventRound(int heldYear, int heldSemester, int eventRound) {
        return eventJpaRepository.existsByHeldYearAndHeldSemesterAndEventRound(heldYear, heldSemester, eventRound);
    }

    @Override
    public HackathonParticipant saveParticipant(HackathonParticipant participant) {
        return participantJpaRepository.save(participant);
    }

    @Override
    public Optional<HackathonParticipant> findParticipant(Long hackathonId, Long userId) {
        return participantJpaRepository.findByHackathonIdAndUserId(hackathonId, userId);
    }

    @Override
    public List<HackathonParticipant> findParticipants(Long hackathonId, ParticipantStatus status) {
        if (status == null) {
            return participantJpaRepository.findByHackathonId(hackathonId);
        }
        return participantJpaRepository.findByHackathonIdAndStatus(hackathonId, status);
    }

    @Override
    public List<HackathonParticipant> findParticipantsWithoutTeam(Long hackathonId, ParticipantStatus status) {
        return participantJpaRepository.findParticipantsWithoutTeam(hackathonId, status);
    }

    @Override
    public HackathonTeam saveTeam(HackathonTeam team) {
        return teamJpaRepository.save(team);
    }

    @Override
    public Optional<HackathonTeam> findTeam(Long hackathonId, Long teamId) {
        return teamJpaRepository.findByHackathonIdAndId(hackathonId, teamId);
    }

    @Override
    public Optional<HackathonTeam> findTeamForUpdate(Long hackathonId, Long teamId) {
        return teamJpaRepository.findWithLockByHackathonIdAndId(hackathonId, teamId);
    }

    @Override
    public List<HackathonTeam> findTeams(Long hackathonId) {
        return teamJpaRepository.findByHackathonIdOrderByIdAsc(hackathonId);
    }

    @Override
    public boolean existsTeamName(Long hackathonId, String name) {
        return teamJpaRepository.existsByHackathonIdAndName(hackathonId, name);
    }

    @Override
    public HackathonTeamMember saveTeamMember(HackathonTeamMember teamMember) {
        return teamMemberJpaRepository.save(teamMember);
    }

    @Override
    public Optional<HackathonTeamMember> findTeamMember(Long hackathonId, Long userId) {
        return teamMemberJpaRepository.findByHackathonIdAndUserId(hackathonId, userId);
    }

    @Override
    public Optional<HackathonTeamMember> findTeamMemberByTeamId(Long teamId, Long userId) {
        return teamMemberJpaRepository.findByTeamIdAndUserId(teamId, userId);
    }

    @Override
    public List<HackathonTeamMember> findTeamMembers(Long teamId) {
        return teamMemberJpaRepository.findByTeamIdOrderByJoinedAtAsc(teamId);
    }

    @Override
    public long countTeamMembers(Long teamId) {
        return teamMemberJpaRepository.countByTeamId(teamId);
    }

    @Override
    public void deleteTeamMembersByTeamId(Long teamId) {
        teamMemberJpaRepository.deleteByTeamId(teamId);
        teamMemberJpaRepository.flush();
    }

    @Override
    public HackathonJoinRequest saveJoinRequest(HackathonJoinRequest joinRequest) {
        return joinRequestJpaRepository.save(joinRequest);
    }

    @Override
    public Optional<HackathonJoinRequest> findJoinRequest(Long hackathonId, Long requestId) {
        return joinRequestJpaRepository.findByHackathonIdAndId(hackathonId, requestId);
    }

    @Override
    public List<HackathonJoinRequest> findJoinRequests(Long teamId, JoinRequestStatus status) {
        if (status == null) {
            return joinRequestJpaRepository.findByTeamIdOrderByCreatedAtAsc(teamId);
        }
        return joinRequestJpaRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(teamId, status);
    }

    @Override
    public boolean existsPendingJoinRequest(Long hackathonId, Long userId) {
        return joinRequestJpaRepository.existsByHackathonIdAndUserIdAndStatus(
                hackathonId, userId, JoinRequestStatus.PENDING);
    }

    @Override
    public HackathonSubmission saveSubmission(HackathonSubmission submission) {
        return submissionJpaRepository.save(submission);
    }

    @Override
    public Optional<HackathonSubmission> findSubmissionByTeam(Long hackathonId, Long teamId) {
        return submissionJpaRepository.findByHackathonIdAndTeamId(hackathonId, teamId);
    }

    @Override
    public Optional<HackathonSubmission> findSubmissionById(Long submissionId) {
        return submissionJpaRepository.findById(submissionId);
    }

    @Override
    public List<HackathonSubmission> findSubmissions(Long hackathonId) {
        return submissionJpaRepository.findByHackathonIdOrderByIdAsc(hackathonId);
    }

    @Override
    public boolean existsSubmissionByTeam(Long hackathonId, Long teamId) {
        return submissionJpaRepository.existsByHackathonIdAndTeamId(hackathonId, teamId);
    }

    @Override
    public void deleteTechStacksBySubmissionId(Long submissionId) {
        techStackJpaRepository.deleteBySubmission_Id(submissionId);
        techStackJpaRepository.flush();
    }

    @Override
    public List<HackathonSubmissionTechStack> saveTechStacks(List<HackathonSubmissionTechStack> techStacks) {
        return techStackJpaRepository.saveAll(techStacks);
    }

    @Override
    public List<HackathonSubmissionTechStack> findTechStacksBySubmissionIds(List<Long> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return List.of();
        }
        return techStackJpaRepository.findBySubmission_IdInOrderByDisplayOrderAsc(submissionIds);
    }

    @Override
    public HackathonEvaluationCriterion saveCriterion(HackathonEvaluationCriterion criterion) {
        return criterionJpaRepository.save(criterion);
    }

    @Override
    public Optional<HackathonEvaluationCriterion> findCriterion(Long hackathonId, Long criterionId) {
        return criterionJpaRepository.findByHackathonIdAndId(hackathonId, criterionId);
    }

    @Override
    public List<HackathonEvaluationCriterion> findCriteria(Long hackathonId) {
        return criterionJpaRepository.findByHackathonIdOrderByDisplayOrderAsc(hackathonId);
    }

    @Override
    public void deleteCriterion(HackathonEvaluationCriterion criterion) {
        criterionJpaRepository.delete(criterion);
    }

    @Override
    public boolean existsEvaluationScoreByCriterionId(Long criterionId) {
        return evaluationScoreJpaRepository.existsByCriterionId(criterionId);
    }

    @Override
    public HackathonEvaluation saveEvaluation(HackathonEvaluation evaluation) {
        return evaluationJpaRepository.saveAndFlush(evaluation);
    }

    @Override
    public Optional<HackathonEvaluation> findEvaluation(Long hackathonId, Long teamId, Long evaluatorId) {
        return evaluationJpaRepository.findByHackathonIdAndTargetTeamIdAndEvaluatorId(hackathonId, teamId, evaluatorId);
    }

    @Override
    public List<HackathonEvaluation> findEvaluations(Long hackathonId) {
        return evaluationJpaRepository.findByHackathonIdOrderByTargetTeamIdAsc(hackathonId);
    }

    @Override
    public void deleteEvaluationScoresByEvaluationId(Long evaluationId) {
        evaluationScoreJpaRepository.deleteByEvaluationId(evaluationId);
        evaluationScoreJpaRepository.flush();
    }

    @Override
    public List<HackathonEvaluationScore> saveEvaluationScores(List<HackathonEvaluationScore> scores) {
        return evaluationScoreJpaRepository.saveAll(scores);
    }

    @Override
    public List<HackathonEvaluationScore> findEvaluationScoresByEvaluationIds(List<Long> evaluationIds) {
        if (evaluationIds == null || evaluationIds.isEmpty()) {
            return List.of();
        }
        return evaluationScoreJpaRepository.findByEvaluationIdIn(evaluationIds);
    }

    @Override
    public HackathonAward saveAward(HackathonAward award) {
        return awardJpaRepository.save(award);
    }

    @Override
    public Optional<HackathonAward> findAward(Long hackathonId, Long awardId) {
        return awardJpaRepository.findByHackathonIdAndId(hackathonId, awardId);
    }

    @Override
    public List<HackathonAward> findAwards(Long hackathonId) {
        return awardJpaRepository.findByHackathonIdOrderByAwardRankAscIdAsc(hackathonId);
    }

    @Override
    public void deleteAward(HackathonAward award) {
        awardJpaRepository.delete(award);
    }
}
