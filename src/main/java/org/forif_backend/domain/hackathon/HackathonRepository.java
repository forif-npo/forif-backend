package org.forif_backend.domain.hackathon;

import java.util.List;
import java.util.Optional;

public interface HackathonRepository {

    HackathonEvent saveEvent(HackathonEvent event);

    Optional<HackathonEvent> findEventById(Long hackathonId);

    List<HackathonEvent> findActiveEvents();

    List<HackathonEvent> findEvents(Integer year, Integer semester, HackathonStatus status);

    int findNextEventRound();

    boolean existsEventSemester(int heldYear, int heldSemester);

    HackathonParticipant saveParticipant(HackathonParticipant participant);

    Optional<HackathonParticipant> findParticipant(Long hackathonId, Long userId);

    List<HackathonParticipant> findParticipants(Long hackathonId, ParticipantStatus status);

    List<HackathonParticipant> findParticipantsWithoutTeam(Long hackathonId, ParticipantStatus status);

    /**
     * 해당 학기에 열린 해커톤에 참가 등록(REGISTERED)한 유저 ID 목록
     */
    List<Long> findRegisteredUserIdsBySemester(int heldYear, int heldSemester);

    HackathonTeam saveTeam(HackathonTeam team);

    Optional<HackathonTeam> findTeam(Long hackathonId, Long teamId);

    Optional<HackathonTeam> findTeamForUpdate(Long hackathonId, Long teamId);

    List<HackathonTeam> findTeams(Long hackathonId);

    boolean existsTeamName(Long hackathonId, String name);

    HackathonTeamMember saveTeamMember(HackathonTeamMember teamMember);

    Optional<HackathonTeamMember> findTeamMember(Long hackathonId, Long userId);

    Optional<HackathonTeamMember> findTeamMemberByTeamId(Long teamId, Long userId);

    List<HackathonTeamMember> findTeamMembers(Long teamId);

    long countTeamMembers(Long teamId);

    void deleteTeamMembersByTeamId(Long teamId);

    HackathonJoinRequest saveJoinRequest(HackathonJoinRequest joinRequest);

    Optional<HackathonJoinRequest> findJoinRequest(Long hackathonId, Long requestId);

    List<HackathonJoinRequest> findJoinRequests(Long teamId, JoinRequestStatus status);

    boolean existsPendingJoinRequest(Long hackathonId, Long userId);

    HackathonSubmission saveSubmission(HackathonSubmission submission);

    Optional<HackathonSubmission> findSubmissionByTeam(Long hackathonId, Long teamId);

    Optional<HackathonSubmission> findSubmissionById(Long submissionId);

    List<HackathonSubmission> findSubmissions(Long hackathonId);

    boolean existsSubmissionByTeam(Long hackathonId, Long teamId);

    void deleteTechStacksBySubmissionId(Long submissionId);

    List<HackathonSubmissionTechStack> saveTechStacks(List<HackathonSubmissionTechStack> techStacks);

    List<HackathonSubmissionTechStack> findTechStacksBySubmissionIds(List<Long> submissionIds);

    HackathonEvaluationCriterion saveCriterion(HackathonEvaluationCriterion criterion);

    Optional<HackathonEvaluationCriterion> findCriterion(Long hackathonId, Long criterionId);

    List<HackathonEvaluationCriterion> findCriteria(Long hackathonId);

    void deleteCriterion(HackathonEvaluationCriterion criterion);

    boolean existsEvaluationScoreByCriterionId(Long criterionId);

    HackathonEvaluation saveEvaluation(HackathonEvaluation evaluation);

    Optional<HackathonEvaluation> findEvaluation(Long hackathonId, Long teamId, Long evaluatorId);

    List<HackathonEvaluation> findEvaluations(Long hackathonId);

    void deleteEvaluationScoresByEvaluationId(Long evaluationId);

    List<HackathonEvaluationScore> saveEvaluationScores(List<HackathonEvaluationScore> scores);

    List<HackathonEvaluationScore> findEvaluationScoresByEvaluationIds(List<Long> evaluationIds);

    HackathonAward saveAward(HackathonAward award);

    Optional<HackathonAward> findAward(Long hackathonId, Long awardId);

    List<HackathonAward> findAwards(Long hackathonId);

    void deleteAward(HackathonAward award);
}
