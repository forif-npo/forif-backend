package org.forif_backend.application.hackathon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.hackathon.*;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.hackathon.dto.ParticipantResponse.ParticipantStudyResponse;
import org.forif_backend.web.hackathon.dto.ParticipantResponse.ParticipantStudyRole;
import org.forif_backend.web.hackathon.dto.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.forif_backend.application.file.FileViewUrls;
import org.forif_backend.application.file.TransactionalFileCleanup;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HackathonService {

    private static final String FILE_CLEANUP_CONTEXT = "해커톤 제출 발표자료";
    private static final int DEFAULT_MAX_SCORE = 5;
    private static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;
    private static final List<HackathonStatus> STATUS_FLOW = List.of(
            HackathonStatus.RECRUITING,
            HackathonStatus.TEAM_BUILDING,
            HackathonStatus.IN_PROGRESS,
            HackathonStatus.JUDGING,
            HackathonStatus.ENDED
    );

    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudyUserRepository studyUserRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final FilePort filePort;

    @Transactional
    public HackathonIdResponse createHackathon(CreateHackathonRequest request) {
        validatePeriod(
                request.recruitStartsAt(),
                request.recruitEndsAt(),
                request.teamBuildingStartsAt(),
                request.teamBuildingEndsAt(),
                request.startsAt(),
                request.endsAt()
        );

        if (hackathonRepository.existsEventSemester(request.heldYear(), request.heldSemester())) {
            throw new ForifException(ErrorCode.HACKATHON_ALREADY_EXISTS);
        }

        int eventRound = hackathonRepository.findNextEventRound();
        HackathonEvent event = HackathonEvent.create(
                request.heldYear(),
                request.heldSemester(),
                eventRound,
                request.title(),
                request.description(),
                request.location(),
                request.recruitStartsAt(),
                request.recruitEndsAt(),
                request.teamBuildingStartsAt(),
                request.teamBuildingEndsAt(),
                request.startsAt(),
                request.endsAt()
        );
        HackathonEvent saved = hackathonRepository.saveEvent(event);
        filePort.createDirectory(hackathonUploadDirectory(saved));
        return new HackathonIdResponse(saved.getId());
    }

    @Transactional
    public CursorPageResponse<HackathonResponse> getHackathons(
            Integer year,
            Integer semester,
            HackathonStatus status,
            Integer cursor,
            Integer page,
            int size
    ) {
        synchronizeHackathonStatuses(now());
        List<HackathonResponse> responses = hackathonRepository.findEvents(year, semester, status).stream()
                .map(HackathonResponse::from)
                .toList();

        return paginate(responses, cursor, page, size, HackathonResponse::hackathonId);
    }

    @Transactional
    public List<HackathonResponse> getHackathons(Integer year, Integer semester, HackathonStatus status) {
        synchronizeHackathonStatuses(now());
        return hackathonRepository.findEvents(year, semester, status).stream()
                .map(HackathonResponse::from)
                .toList();
    }

    @Transactional
    public HackathonDetailResponse getHackathon(Long hackathonId) {
        return HackathonDetailResponse.from(getEvent(hackathonId), now());
    }

    @Transactional
    public HackathonDetailResponse updateHackathon(Long hackathonId, UpdateHackathonRequest request) {
        HackathonEvent event = getEvent(hackathonId);
        validatePeriod(
                request.recruitStartsAt() != null ? request.recruitStartsAt() : event.getRecruitStartsAt(),
                request.recruitEndsAt() != null ? request.recruitEndsAt() : event.getRecruitEndsAt(),
                request.teamBuildingStartsAt() != null ? request.teamBuildingStartsAt() : event.getTeamBuildingStartsAt(),
                request.teamBuildingEndsAt() != null ? request.teamBuildingEndsAt() : event.getTeamBuildingEndsAt(),
                request.startsAt() != null ? request.startsAt() : event.getStartsAt(),
                request.endsAt() != null ? request.endsAt() : event.getEndsAt()
        );

        event.update(
                request.title(),
                request.description(),
                request.location(),
                request.recruitStartsAt(),
                request.recruitEndsAt(),
                request.teamBuildingStartsAt(),
                request.teamBuildingEndsAt(),
                request.startsAt(),
                request.endsAt()
        );
        return HackathonDetailResponse.from(event, now());
    }

    @Transactional
    public void changeHackathonStatus(Long hackathonId, HackathonStatus status) {
        HackathonEvent event = getEvent(hackathonId);
        assertNextStatus(event.getStatus(), status);
        event.changeStatus(status);
    }

    @Transactional
    public void deleteHackathon(Long hackathonId) {
        getEvent(hackathonId).delete(now());
    }

    @Scheduled(
            initialDelayString = "${hackathon.status-sync.initial-delay-ms:30000}",
            fixedDelayString = "${hackathon.status-sync.fixed-delay-ms:30000}"
    )
    @Transactional
    public void synchronizeHackathonStatuses() {
        synchronizeHackathonStatuses(now());
    }

    private void synchronizeHackathonStatuses(LocalDateTime now) {
        hackathonRepository.findActiveEvents()
                .forEach(event -> promoteHackathonStatusBySchedule(event, now));
    }

    @Transactional
    public ParticipantResponse registerParticipant(Long hackathonId, Long userId) {
        HackathonEvent event = getEvent(hackathonId);
        LocalDateTime now = now();
        assertRegistrationOpen(event, now);
        assertStatus(event, HackathonStatus.RECRUITING);

        User user = getUser(userId);
        if (!canRegister(event, userId)) {
            throw new ForifException(ErrorCode.HACKATHON_PARTICIPATION_NOT_ALLOWED);
        }

        Optional<HackathonParticipant> existing = hackathonRepository.findParticipant(hackathonId, userId);
        if (existing.isPresent()) {
            HackathonParticipant participant = existing.get();
            if (participant.getStatus() == ParticipantStatus.REGISTERED) {
                throw new ForifException(ErrorCode.HACKATHON_ALREADY_REGISTERED);
            }
            participant.registerAgain(now);
            return ParticipantResponse.from(participant);
        }

        HackathonParticipant participant = HackathonParticipant.register(event, user, now);
        return ParticipantResponse.from(hackathonRepository.saveParticipant(participant));
    }

    @Transactional
    public void cancelParticipant(Long hackathonId, Long userId) {
        HackathonEvent event = getEvent(hackathonId);
        if (event.getStatus() != HackathonStatus.RECRUITING && event.getStatus() != HackathonStatus.TEAM_BUILDING) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_STATUS);
        }
        if (hackathonRepository.findTeamMember(hackathonId, userId).isPresent()) {
            throw new ForifException(ErrorCode.HACKATHON_ALREADY_TEAM_MEMBER);
        }
        HackathonParticipant participant = hackathonRepository.findParticipant(hackathonId, userId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_PARTICIPANT_REQUIRED));
        participant.cancel(now());
    }

    public ParticipantResponse getMyParticipant(Long hackathonId, Long userId) {
        return hackathonRepository.findParticipant(hackathonId, userId)
                .map(ParticipantResponse::from)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_PARTICIPANT_REQUIRED));
    }

    public CursorPageResponse<ParticipantResponse> getParticipants(
            Long hackathonId,
            ParticipantStatus status,
            boolean withoutTeam,
            Integer cursor,
            Integer page,
            int size
    ) {
        return paginate(getParticipants(hackathonId, status, withoutTeam), cursor, page, size, ParticipantResponse::participantId);
    }

    public List<ParticipantResponse> getParticipants(Long hackathonId, ParticipantStatus status, boolean withoutTeam) {
        HackathonEvent event = getEvent(hackathonId);
        List<HackathonParticipant> participants = withoutTeam
                ? hackathonRepository.findParticipantsWithoutTeam(hackathonId, status)
                : hackathonRepository.findParticipants(hackathonId, status);
        Map<Long, List<ParticipantStudyResponse>> studiesByUserId = getParticipantStudiesByUserId(participants, event);
        return participants.stream()
                .map(participant -> ParticipantResponse.from(
                        participant,
                        studiesByUserId.getOrDefault(participant.getUser().getId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<ParticipantStudyResponse>> getParticipantStudiesByUserId(
            List<HackathonParticipant> participants,
            HackathonEvent event
    ) {
        List<Long> userIds = participants.stream()
                .map(participant -> participant.getUser().getId())
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Study>> menteeStudies = studyRepository.findCurrentStudiesByUserIds(
                userIds,
                event.getHeldYear(),
                event.getHeldSemester()
        );
        Map<Long, List<Study>> mentorStudies = studyRepository.findCurrentMentorStudiesByUserIds(
                userIds,
                event.getHeldYear(),
                event.getHeldSemester()
        );

        Map<Long, List<ParticipantStudyResponse>> studiesByUserId = new HashMap<>();
        for (Long userId : userIds) {
            List<ParticipantStudyResponse> studies = new ArrayList<>();
            menteeStudies.getOrDefault(userId, List.of())
                    .forEach(study -> studies.add(ParticipantStudyResponse.of(study, ParticipantStudyRole.MENTEE)));
            mentorStudies.getOrDefault(userId, List.of())
                    .forEach(study -> studies.add(ParticipantStudyResponse.of(study, ParticipantStudyRole.MENTOR)));

            if (!studies.isEmpty()) {
                studiesByUserId.put(userId, studies);
            }
        }

        return studiesByUserId;
    }

    @Transactional
    public TeamResponse createTeam(Long hackathonId, Long userId, CreateTeamRequest request) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.TEAM_BUILDING);
        assertRegisteredParticipant(hackathonId, userId);
        assertNoTeam(hackathonId, userId);

        if (hackathonRepository.existsTeamName(hackathonId, request.name())) {
            throw new ForifException(ErrorCode.HACKATHON_TEAM_NAME_ALREADY_EXISTS);
        }

        User leader = getUser(userId);
        HackathonTeam team = HackathonTeam.create(
                event,
                leader,
                request.name(),
                request.topic(),
                request.description(),
                request.competitionType(),
                request.maxMembers()
        );
        HackathonTeam savedTeam = hackathonRepository.saveTeam(team);
        hackathonRepository.saveTeamMember(HackathonTeamMember.createLeader(event, savedTeam, leader, now()));
        return toTeamResponse(savedTeam);
    }

    public CursorPageResponse<TeamResponse> getTeams(Long hackathonId, Integer cursor, Integer page, int size) {
        return paginate(getTeams(hackathonId), cursor, page, size, TeamResponse::hackathonTeamId);
    }

    public List<TeamResponse> getTeams(Long hackathonId) {
        getEvent(hackathonId);
        return getTeamResponses(hackathonId);
    }

    public CursorPageResponse<TeamResponse> getTeamsForParticipant(
            Long hackathonId,
            Long userId,
            Integer cursor,
            Integer page,
            int size
    ) {
        return paginate(getTeamsForParticipant(hackathonId, userId), cursor, page, size, TeamResponse::hackathonTeamId);
    }

    public List<TeamResponse> getTeamsForParticipant(Long hackathonId, Long userId) {
        getEvent(hackathonId);
        assertRegisteredParticipant(hackathonId, userId);
        return getTeamResponses(hackathonId);
    }

    private List<TeamResponse> getTeamResponses(Long hackathonId) {
        return hackathonRepository.findTeams(hackathonId).stream()
                .map(this::toTeamResponse)
                .toList();
    }

    public TeamResponse getMyTeam(Long hackathonId, Long userId) {
        HackathonTeamMember member = hackathonRepository.findTeamMember(hackathonId, userId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_TEAM_NOT_FOUND));
        return toTeamResponse(member.getTeam());
    }

    @Transactional
    public TeamResponse updateTeam(Long hackathonId, Long teamId, Long userId, UpdateTeamRequest request) {
        HackathonEvent event = getEvent(hackathonId);
        assertTeamEditableStatus(event);
        HackathonTeam team = getTeamOrThrow(hackathonId, teamId);
        assertTeamLeader(team, userId);

        if (request.name() != null && !request.name().equals(team.getName())
                && hackathonRepository.existsTeamName(hackathonId, request.name())) {
            throw new ForifException(ErrorCode.HACKATHON_TEAM_NAME_ALREADY_EXISTS);
        }

        team.update(
                request.name(),
                request.topic(),
                request.description(),
                request.competitionType(),
                request.maxMembers()
        );
        return toTeamResponse(team);
    }

    @Transactional
    public void disbandTeam(Long hackathonId, Long teamId, Long userId) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.TEAM_BUILDING);
        HackathonTeam team = getTeamOrThrow(hackathonId, teamId);
        assertTeamLeader(team, userId);
        disbandTeam(team);
    }

    @Transactional
    public void deleteTeamByAdmin(Long hackathonId, Long teamId) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.TEAM_BUILDING);
        disbandTeam(getTeamOrThrow(hackathonId, teamId));
    }

    @Transactional
    public JoinRequestResponse createJoinRequest(Long hackathonId, Long teamId, Long userId, CreateJoinRequest request) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.TEAM_BUILDING);
        assertRegisteredParticipant(hackathonId, userId);
        assertNoTeam(hackathonId, userId);

        HackathonTeam team = getTeamOrThrow(hackathonId, teamId);
        assertTeamCapacity(team);
        if (hackathonRepository.existsPendingJoinRequest(hackathonId, userId)) {
            throw new ForifException(ErrorCode.HACKATHON_JOIN_REQUEST_ALREADY_EXISTS);
        }

        HackathonJoinRequest joinRequest = HackathonJoinRequest.create(event, team, getUser(userId), request.message());
        return JoinRequestResponse.from(hackathonRepository.saveJoinRequest(joinRequest));
    }

    public List<JoinRequestResponse> getJoinRequests(Long hackathonId, Long teamId, Long userId, JoinRequestStatus status) {
        HackathonTeam team = getTeamOrThrow(hackathonId, teamId);
        assertTeamLeader(team, userId);
        return hackathonRepository.findJoinRequests(teamId, status).stream()
                .map(JoinRequestResponse::from)
                .toList();
    }

    public CursorPageResponse<JoinRequestResponse> getJoinRequests(
            Long hackathonId,
            Long teamId,
            Long userId,
            JoinRequestStatus status,
            Integer cursor,
            Integer page,
            int size
    ) {
        return paginate(
                getJoinRequests(hackathonId, teamId, userId, status),
                cursor,
                page,
                size,
                JoinRequestResponse::joinRequestId
        );
    }

    @Transactional
    public JoinRequestResponse approveJoinRequest(Long hackathonId, Long requestId, Long userId) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.TEAM_BUILDING);
        HackathonJoinRequest request = getJoinRequestOrThrow(hackathonId, requestId);
        HackathonTeam team = getTeamForUpdateOrThrow(hackathonId, request.getTeam().getId());
        assertTeamLeader(team, userId);
        assertPendingJoinRequest(request);
        assertNoTeam(hackathonId, request.getUser().getId());
        assertTeamCapacity(team);

        User reviewer = getUser(userId);
        request.approve(reviewer, now());
        hackathonRepository.saveTeamMember(HackathonTeamMember.createMember(event, team, request.getUser(), now()));
        return JoinRequestResponse.from(request);
    }

    @Transactional
    public JoinRequestResponse rejectJoinRequest(Long hackathonId, Long requestId, Long userId) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.TEAM_BUILDING);
        HackathonJoinRequest request = getJoinRequestOrThrow(hackathonId, requestId);
        assertTeamLeader(request.getTeam(), userId);
        assertPendingJoinRequest(request);

        request.reject(getUser(userId), now());
        return JoinRequestResponse.from(request);
    }

    @Transactional
    public SubmissionResponse createSubmission(Long hackathonId, Long teamId, Long userId,
                                               SubmissionRequest request, MultipartFile presentation) {
        HackathonEvent event = getEvent(hackathonId);
        assertSubmissionOpen(event);
        HackathonTeam team = getTeamOrThrow(hackathonId, teamId);
        assertTeamLeader(team, userId);

        if (hackathonRepository.existsSubmissionByTeam(hackathonId, teamId)) {
            throw new ForifException(ErrorCode.HACKATHON_SUBMISSION_ALREADY_EXISTS);
        }

        String presentationFile = presentation != null && !presentation.isEmpty()
                ? uploadPresentation(presentation, event)
                : null;
        HackathonSubmission submission = HackathonSubmission.create(
                event,
                team,
                request.projectName(),
                request.summary(),
                request.description(),
                request.githubUrl(),
                request.deployUrl(),
                request.imageUrl(),
                presentationFile
        );
        HackathonSubmission saved = hackathonRepository.saveSubmission(submission);
        replaceTechStacks(saved, request.techStacks());
        return toSubmissionResponse(saved);
    }

    @Transactional
    public SubmissionResponse updateSubmission(Long hackathonId, Long teamId, Long userId,
                                               SubmissionRequest request, MultipartFile presentation) {
        HackathonEvent event = getEvent(hackathonId);
        assertSubmissionOpen(event);
        HackathonTeam team = getTeamOrThrow(hackathonId, teamId);
        assertTeamLeader(team, userId);

        HackathonSubmission submission = hackathonRepository.findSubmissionByTeam(hackathonId, teamId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_SUBMISSION_NOT_FOUND));

        String previousPresentationFile = submission.getPresentationFile();
        String presentationFile = presentation != null && !presentation.isEmpty()
                ? uploadPresentation(presentation, event)
                : previousPresentationFile;
        submission.update(
                request.projectName(),
                request.summary(),
                request.description(),
                request.githubUrl(),
                request.deployUrl(),
                request.imageUrl(),
                presentationFile
        );
        if (request.techStacks() != null) {
            replaceTechStacks(submission, request.techStacks());
        }
        if (presentation != null && !presentation.isEmpty()
                && previousPresentationFile != null
                && !previousPresentationFile.equals(presentationFile)) {
            deleteFileAfterCommit(previousPresentationFile);
        }
        return toSubmissionResponse(submission);
    }

    public List<SubmissionResponse> getSubmissions(Long hackathonId) {
        getEvent(hackathonId);
        List<HackathonSubmission> submissions = hackathonRepository.findSubmissions(hackathonId);
        Map<Long, List<String>> techStacks = techStacksBySubmissionId(submissions);
        return submissions.stream()
                .map(submission -> toSubmissionResponse(submission, techStacks.getOrDefault(submission.getId(), List.of())))
                .toList();
    }

    public CursorPageResponse<SubmissionResponse> getSubmissions(
            Long hackathonId,
            Integer cursor,
            Integer page,
            int size
    ) {
        return paginate(getSubmissions(hackathonId), cursor, page, size, SubmissionResponse::submissionId);
    }

    @Transactional
    public CriterionResponse createCriterion(Long hackathonId, CriterionRequest request) {
        HackathonEvent event = getEvent(hackathonId);
        HackathonEvaluationCriterion criterion = HackathonEvaluationCriterion.create(
                event,
                request.name(),
                request.description(),
                request.maxScore() != null ? request.maxScore() : DEFAULT_MAX_SCORE,
                request.weight() != null ? request.weight() : DEFAULT_WEIGHT,
                request.displayOrder()
        );
        return CriterionResponse.from(hackathonRepository.saveCriterion(criterion));
    }

    @Transactional
    public CriterionResponse updateCriterion(Long hackathonId, Long criterionId, CriterionRequest request) {
        HackathonEvaluationCriterion criterion = getCriterionOrThrow(hackathonId, criterionId);
        criterion.update(request.name(), request.description(), request.maxScore(), request.weight(), request.displayOrder());
        return CriterionResponse.from(criterion);
    }

    @Transactional
    public void deleteCriterion(Long hackathonId, Long criterionId) {
        HackathonEvaluationCriterion criterion = getCriterionOrThrow(hackathonId, criterionId);
        if (hackathonRepository.existsEvaluationScoreByCriterionId(criterionId)) {
            throw new ForifException(ErrorCode.HACKATHON_EVALUATION_CRITERION_HAS_SCORES);
        }
        hackathonRepository.deleteCriterion(criterion);
    }

    public List<CriterionResponse> getCriteria(Long hackathonId) {
        getEvent(hackathonId);
        return hackathonRepository.findCriteria(hackathonId).stream()
                .map(CriterionResponse::from)
                .toList();
    }

    public CursorPageResponse<CriterionResponse> getCriteria(Long hackathonId, Integer cursor, Integer page, int size) {
        return paginate(getCriteria(hackathonId), cursor, page, size, CriterionResponse::criterionId);
    }

    @Transactional
    public EvaluationResponse createEvaluation(Long hackathonId, Long teamId, Long evaluatorId, EvaluationRequest request) {
        if (hackathonRepository.findEvaluation(hackathonId, teamId, evaluatorId).isPresent()) {
            throw new ForifException(ErrorCode.HACKATHON_EVALUATION_ALREADY_EXISTS);
        }
        try {
            return saveEvaluation(hackathonId, teamId, evaluatorId, request, false);
        } catch (DataIntegrityViolationException e) {
            throw new ForifException(ErrorCode.HACKATHON_EVALUATION_ALREADY_EXISTS);
        }
    }

    @Transactional
    public EvaluationResponse updateMyEvaluation(Long hackathonId, Long teamId, Long evaluatorId, EvaluationRequest request) {
        return saveEvaluation(hackathonId, teamId, evaluatorId, request, true);
    }

    public EvaluationResponse getMyEvaluation(Long hackathonId, Long teamId, Long evaluatorId) {
        HackathonEvaluation evaluation = hackathonRepository.findEvaluation(hackathonId, teamId, evaluatorId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_EVALUATION_NOT_FOUND));
        return toEvaluationResponse(evaluation);
    }

    public List<EvaluationSummaryResponse> getEvaluationSummary(Long hackathonId) {
        getEvent(hackathonId);
        List<HackathonEvaluation> evaluations = hackathonRepository.findEvaluations(hackathonId);
        List<HackathonEvaluationScore> scores = hackathonRepository.findEvaluationScoresByEvaluationIds(
                evaluations.stream().map(HackathonEvaluation::getId).toList());
        Map<Long, HackathonEvaluationCriterion> criteria = hackathonRepository.findCriteria(hackathonId).stream()
                .collect(Collectors.toMap(HackathonEvaluationCriterion::getId, Function.identity()));

        Map<Long, List<HackathonEvaluation>> evaluationsByTeam = evaluations.stream()
                .collect(Collectors.groupingBy(evaluation -> evaluation.getTargetTeam().getId()));
        Map<Long, HackathonEvaluation> evaluationById = evaluations.stream()
                .collect(Collectors.toMap(HackathonEvaluation::getId, Function.identity()));

        return evaluationsByTeam.entrySet().stream()
                .map(entry -> toSummary(entry.getKey(), entry.getValue(), scores, evaluationById, criteria))
                .sorted(Comparator.comparing(EvaluationSummaryResponse::averageTotalScore).reversed())
                .toList();
    }

    public CursorPageResponse<EvaluationSummaryResponse> getEvaluationSummary(
            Long hackathonId,
            Integer cursor,
            Integer page,
            int size
    ) {
        return paginate(getEvaluationSummary(hackathonId), cursor, page, size, EvaluationSummaryResponse::teamId);
    }

    @Transactional
    public AwardResponse createAward(Long hackathonId, AwardRequest request) {
        HackathonEvent event = getEvent(hackathonId);
        HackathonTeam team = getTeamOrThrow(hackathonId, request.hackathonTeamId());
        HackathonAward award = HackathonAward.create(event, team, request.awardName(), request.awardRank());
        return AwardResponse.from(hackathonRepository.saveAward(award));
    }

    @Transactional
    public AwardResponse updateAward(Long hackathonId, Long awardId, AwardRequest request) {
        HackathonAward award = getAwardOrThrow(hackathonId, awardId);
        award.update(request.awardName(), request.awardRank());
        return AwardResponse.from(award);
    }

    @Transactional
    public void deleteAward(Long hackathonId, Long awardId) {
        hackathonRepository.deleteAward(getAwardOrThrow(hackathonId, awardId));
    }

    public List<AwardResponse> getAwards(Long hackathonId) {
        getEvent(hackathonId);
        return hackathonRepository.findAwards(hackathonId).stream()
                .map(AwardResponse::from)
                .toList();
    }

    public CursorPageResponse<AwardResponse> getAwards(Long hackathonId, Integer cursor, Integer page, int size) {
        return paginate(getAwards(hackathonId), cursor, page, size, AwardResponse::awardId);
    }

    public List<HackathonResponse> getArchiveHackathons(Integer year, Integer semester) {
        return hackathonRepository.findEvents(year, semester, HackathonStatus.ENDED).stream()
                .map(HackathonResponse::from)
                .toList();
    }

    public CursorPageResponse<HackathonResponse> getArchiveHackathons(
            Integer year,
            Integer semester,
            Integer cursor,
            Integer page,
            int size
    ) {
        return paginate(getArchiveHackathons(year, semester), cursor, page, size, HackathonResponse::hackathonId);
    }

    public ArchiveHackathonDetailResponse getArchiveHackathon(Long hackathonId) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.ENDED);
        long participantCount = hackathonRepository.findParticipants(hackathonId, ParticipantStatus.REGISTERED).size();
        long teamCount = hackathonRepository.findTeams(hackathonId).stream()
                .filter(team -> team.getStatus() != TeamStatus.DISBANDED)
                .count();
        long submissionCount = hackathonRepository.findSubmissions(hackathonId).size();
        List<AwardResponse> awards = hackathonRepository.findAwards(hackathonId).stream()
                .map(AwardResponse::from)
                .toList();
        return ArchiveHackathonDetailResponse.of(event, participantCount, teamCount, submissionCount, awards);
    }

    public List<SubmissionResponse> getArchiveSubmissions(Long hackathonId, String search, String techStack) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.ENDED);
        List<HackathonSubmission> submissions = hackathonRepository.findSubmissions(hackathonId);
        Map<Long, List<HackathonSubmissionTechStack>> techStackEntities = techStackEntitiesBySubmissionId(submissions);
        Map<Long, List<String>> techStacks = techStackNamesBySubmissionId(techStackEntities);
        Map<Long, Integer> awardPriorityByTeamId = archiveAwardPriorityByTeamId(hackathonId);
        return submissions.stream()
                .filter(submission -> matchesSubmissionSearch(submission, search))
                .filter(submission -> matchesTechStack(
                        techStackEntities.getOrDefault(submission.getId(), List.of()), techStack))
                .sorted(Comparator
                        .comparingInt((HackathonSubmission submission) ->
                                awardPriorityByTeamId.getOrDefault(submission.getTeam().getId(), Integer.MAX_VALUE))
                        .thenComparing(HackathonSubmission::getId))
                .map(submission -> toSubmissionResponse(submission, techStacks.getOrDefault(submission.getId(), List.of())))
                .toList();
    }

    public CursorPageResponse<SubmissionResponse> getArchiveSubmissions(
            Long hackathonId,
            String search,
            String techStack,
            Integer cursor,
            Integer page,
            int size
    ) {
        return paginate(
                getArchiveSubmissions(hackathonId, search, techStack),
                cursor,
                page,
                size,
                SubmissionResponse::submissionId
        );
    }

    public ArchiveSubmissionDetailResponse getArchiveSubmission(Long submissionId) {
        HackathonSubmission submission = hackathonRepository.findSubmissionById(submissionId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_SUBMISSION_NOT_FOUND));
        assertStatus(submission.getHackathon(), HackathonStatus.ENDED);
        List<String> techStacks = techStacksBySubmissionId(List.of(submission))
                .getOrDefault(submission.getId(), List.of());
        List<TeamMemberResponse> teamMembers = hackathonRepository.findTeamMembers(submission.getTeam().getId()).stream()
                .map(TeamMemberResponse::from)
                .toList();
        List<AwardResponse> awards = hackathonRepository.findAwards(submission.getHackathon().getId()).stream()
                .filter(award -> award.getTeam().getId().equals(submission.getTeam().getId()))
                .map(AwardResponse::from)
                .toList();
        return ArchiveSubmissionDetailResponse.of(submission, techStacks, teamMembers, awards, toFileViewUrl(submission.getPresentationFile()));
    }

    public List<SubmissionStatusResponse> getSubmissionStatuses(Long hackathonId) {
        getEvent(hackathonId);
        List<HackathonTeam> teams = hackathonRepository.findTeams(hackathonId).stream()
                .filter(team -> team.getStatus() != TeamStatus.DISBANDED)
                .toList();
        List<HackathonSubmission> submissions = hackathonRepository.findSubmissions(hackathonId);
        Map<Long, HackathonSubmission> submissionByTeamId = submissions.stream()
                .collect(Collectors.toMap(submission -> submission.getTeam().getId(), Function.identity()));
        Map<Long, List<String>> techStacks = techStacksBySubmissionId(submissions);

        return teams.stream()
                .map(team -> {
                    HackathonSubmission submission = submissionByTeamId.get(team.getId());
                    List<String> submissionTechStacks = submission != null
                            ? techStacks.getOrDefault(submission.getId(), List.of())
                            : List.of();
                    return SubmissionStatusResponse.of(
                            team,
                            hackathonRepository.countTeamMembers(team.getId()),
                            submission != null ? toSubmissionResponse(submission, submissionTechStacks) : null
                    );
                })
                .toList();
    }

    public CursorPageResponse<SubmissionStatusResponse> getSubmissionStatuses(
            Long hackathonId,
            Integer cursor,
            Integer page,
            int size
    ) {
        return paginate(
                getSubmissionStatuses(hackathonId),
                cursor,
                page,
                size,
                SubmissionStatusResponse::hackathonTeamId
        );
    }

    private EvaluationResponse saveEvaluation(Long hackathonId, Long teamId, Long evaluatorId,
                                              EvaluationRequest request, boolean update) {
        HackathonEvent event = getEvent(hackathonId);
        assertStatus(event, HackathonStatus.JUDGING);
        HackathonTeam targetTeam = getTeamOrThrow(hackathonId, teamId);
        if (!hackathonRepository.existsSubmissionByTeam(hackathonId, teamId)) {
            throw new ForifException(ErrorCode.HACKATHON_SUBMISSION_NOT_FOUND);
        }

        EvaluatorType evaluatorType = resolveEvaluatorType(hackathonId, teamId, evaluatorId);
        ScoreValidationResult validation = validateScores(hackathonId, request);
        LocalDateTime now = now();

        Optional<HackathonEvaluation> existing = hackathonRepository.findEvaluation(hackathonId, teamId, evaluatorId);
        HackathonEvaluation evaluation;
        if (update) {
            evaluation = existing.orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_EVALUATION_NOT_FOUND));
            evaluation.update(validation.totalScore(), now);
            hackathonRepository.deleteEvaluationScoresByEvaluationId(evaluation.getId());
        } else {
            evaluation = HackathonEvaluation.create(event, targetTeam, getUser(evaluatorId), evaluatorType, validation.totalScore(), now);
            evaluation = hackathonRepository.saveEvaluation(evaluation);
        }

        HackathonEvaluation savedEvaluation = evaluation;
        List<HackathonEvaluationScore> scores = request.scores().stream()
                .map(score -> HackathonEvaluationScore.create(
                        savedEvaluation,
                        validation.criteriaById().get(score.criterionId()),
                        score.score()
                ))
                .toList();
        hackathonRepository.saveEvaluationScores(scores);
        return toEvaluationResponse(savedEvaluation);
    }

    private EvaluatorType resolveEvaluatorType(Long hackathonId, Long targetTeamId, Long evaluatorId) {
        Optional<StaffAccount> staff = staffAccountRepository.findByUserId(evaluatorId);
        if (staff.isPresent() && staff.get().getRole() == StaffRole.ADMIN) {
            return EvaluatorType.ADMIN;
        }

        HackathonTeamMember member = hackathonRepository.findTeamMember(hackathonId, evaluatorId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_EVALUATION_NOT_ALLOWED));
        if (member.getTeam().getId().equals(targetTeamId)) {
            throw new ForifException(ErrorCode.HACKATHON_SELF_EVALUATION_NOT_ALLOWED);
        }
        return EvaluatorType.PARTICIPANT;
    }

    private ScoreValidationResult validateScores(Long hackathonId, EvaluationRequest request) {
        List<HackathonEvaluationCriterion> criteria = hackathonRepository.findCriteria(hackathonId);
        if (criteria.isEmpty()) {
            throw new ForifException(ErrorCode.HACKATHON_EVALUATION_CRITERIA_REQUIRED);
        }

        Map<Long, HackathonEvaluationCriterion> criteriaById = criteria.stream()
                .collect(Collectors.toMap(HackathonEvaluationCriterion::getId, Function.identity()));
        Set<Long> requestCriterionIds = request.scores().stream()
                .map(EvaluationRequest.Score::criterionId)
                .collect(Collectors.toSet());

        if (request.scores().size() != criteria.size() || requestCriterionIds.size() != criteria.size()
                || !requestCriterionIds.equals(criteriaById.keySet())) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_EVALUATION_SCORE);
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        for (EvaluationRequest.Score score : request.scores()) {
            HackathonEvaluationCriterion criterion = criteriaById.get(score.criterionId());
            if (score.score() < 1 || score.score() > criterion.getMaxScore()) {
                throw new ForifException(ErrorCode.HACKATHON_INVALID_EVALUATION_SCORE);
            }
            totalScore = totalScore.add(BigDecimal.valueOf(score.score()).multiply(criterion.getWeight()));
        }
        return new ScoreValidationResult(criteriaById, totalScore);
    }

    private void replaceTechStacks(HackathonSubmission submission, List<String> techStacks) {
        hackathonRepository.deleteTechStacksBySubmissionId(submission.getId());
        if (techStacks == null || techStacks.isEmpty()) {
            return;
        }

        List<String> canonicalTechStacks = techStacks.stream()
                .filter(Objects::nonNull)
                .map(HackathonTechStackPolicy::canonicalize)
                .filter(stack -> !stack.isBlank())
                .toList();

        if (canonicalTechStacks.stream().anyMatch(stack -> !HackathonTechStackPolicy.isValid(stack))) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_TECH_STACK);
        }

        Map<String, String> techStackNameByNormalized = canonicalTechStacks.stream()
                .collect(Collectors.toMap(
                        HackathonTechStackPolicy::normalize,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        if (techStackNameByNormalized.size() > HackathonTechStackPolicy.MAX_COUNT) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_TECH_STACK);
        }

        List<HackathonSubmissionTechStack> entities = new ArrayList<>();
        int displayOrder = 1;
        for (Map.Entry<String, String> entry : techStackNameByNormalized.entrySet()) {
            entities.add(HackathonSubmissionTechStack.create(
                    submission,
                    entry.getValue(),
                    entry.getKey(),
                    displayOrder++
            ));
        }
        hackathonRepository.saveTechStacks(entities);
    }

    private <T> CursorPageResponse<T> paginate(
            List<T> items,
            Integer cursor,
            Integer page,
            int size,
            Function<T, Long> cursorExtractor
    ) {
        int pageSize = Math.max(size, 1);
        long totalElements = items.size();

        if (page != null) {
            int currentPage = Math.max(page, 0);
            int fromIndex = Math.min(currentPage * pageSize, items.size());
            int toIndex = Math.min(fromIndex + pageSize, items.size());
            List<T> content = items.subList(fromIndex, toIndex);
            boolean hasNext = toIndex < items.size();
            return CursorPageResponse.ofOffset(content, hasNext, totalElements, currentPage, pageSize);
        }

        int startIndex = resolveCursorStartIndex(items, cursor, cursorExtractor);
        int fromIndex = Math.min(startIndex, items.size());
        int toIndex = Math.min(fromIndex + pageSize + 1, items.size());
        List<T> window = items.subList(fromIndex, toIndex);
        boolean hasNext = window.size() > pageSize;
        List<T> content = hasNext ? window.subList(0, pageSize) : window;
        Integer nextCursor = hasNext && !content.isEmpty()
                ? toIntegerCursor(cursorExtractor.apply(content.get(content.size() - 1)))
                : null;
        return CursorPageResponse.ofCursor(content, nextCursor, hasNext, totalElements);
    }

    private <T> int resolveCursorStartIndex(List<T> items, Integer cursor, Function<T, Long> cursorExtractor) {
        if (cursor == null) {
            return 0;
        }
        if (items.isEmpty()) {
            return 0;
        }
        Long cursorValue = cursor.longValue();
        for (int i = 0; i < items.size(); i++) {
            Long itemCursor = cursorExtractor.apply(items.get(i));
            if (itemCursor != null && itemCursor.equals(cursorValue)) {
                return i + 1;
            }
        }

        Long firstCursor = cursorExtractor.apply(items.get(0));
        Long lastCursor = cursorExtractor.apply(items.get(items.size() - 1));
        if (firstCursor == null || lastCursor == null) {
            return items.size();
        }

        boolean descending = firstCursor > lastCursor;
        for (int i = 0; i < items.size(); i++) {
            Long itemCursor = cursorExtractor.apply(items.get(i));
            if (itemCursor == null) {
                continue;
            }
            if (descending && itemCursor < cursorValue) {
                return i;
            }
            if (!descending && itemCursor > cursorValue) {
                return i;
            }
        }
        return items.size();
    }

    private Integer toIntegerCursor(Long cursor) {
        return cursor != null ? cursor.intValue() : null;
    }

    private void deleteFileAfterCommit(String objectKey) {
        TransactionalFileCleanup.deleteAfterCommit(filePort, objectKey, FILE_CLEANUP_CONTEXT);
    }

    /** 저장이 롤백되면 방금 올린 발표자료가 고아로 남지 않게 회수한다. */
    private String uploadPresentation(MultipartFile presentation, HackathonEvent event) {
        String objectKey = filePort.uploadFile(presentation, hackathonUploadDirectory(event));
        TransactionalFileCleanup.deleteOnRollback(filePort, objectKey, FILE_CLEANUP_CONTEXT);
        return objectKey;
    }

    private void disbandTeam(HackathonTeam team) {
        team.disband();
        hackathonRepository.findJoinRequests(team.getId(), JoinRequestStatus.PENDING)
                .forEach(HackathonJoinRequest::cancel);
        hackathonRepository.deleteTeamMembersByTeamId(team.getId());
    }

    private HackathonEvent getEvent(Long hackathonId) {
        HackathonEvent event = hackathonRepository.findEventById(hackathonId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_NOT_FOUND));
        promoteHackathonStatusBySchedule(event, now());
        return event;
    }

    private User getUser(Long userId) {
        return userRepository.findUserById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));
    }

    private HackathonTeam getTeamOrThrow(Long hackathonId, Long teamId) {
        return hackathonRepository.findTeam(hackathonId, teamId)
                .filter(team -> team.getStatus() != TeamStatus.DISBANDED)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_TEAM_NOT_FOUND));
    }

    private HackathonTeam getTeamForUpdateOrThrow(Long hackathonId, Long teamId) {
        return hackathonRepository.findTeamForUpdate(hackathonId, teamId)
                .filter(team -> team.getStatus() != TeamStatus.DISBANDED)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_TEAM_NOT_FOUND));
    }

    private HackathonJoinRequest getJoinRequestOrThrow(Long hackathonId, Long requestId) {
        return hackathonRepository.findJoinRequest(hackathonId, requestId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_JOIN_REQUEST_NOT_FOUND));
    }

    private HackathonEvaluationCriterion getCriterionOrThrow(Long hackathonId, Long criterionId) {
        return hackathonRepository.findCriterion(hackathonId, criterionId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_EVALUATION_CRITERION_NOT_FOUND));
    }

    private HackathonAward getAwardOrThrow(Long hackathonId, Long awardId) {
        return hackathonRepository.findAward(hackathonId, awardId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_AWARD_NOT_FOUND));
    }

    private boolean canRegister(HackathonEvent event, Long userId) {
        // 운영진은 ADMIN 계정 보유로 판정한다. 회장이 매 학기 물러난 운영진 계정을
        // 정리하므로 사실상 현재 학기 운영진과 같다. MENTOR 행까지 세면 역대 멘토가
        // 전부 영구 자격을 갖게 된다.
        return staffAccountRepository.existsByUserIdAndRole(userId, StaffRole.ADMIN)
                || studyUserRepository.existsByUserIdAndStudyYearSemester(
                        userId, event.getHeldYear(), event.getHeldSemester()
                )
                || studyRepository.existsMentorStudyByMentorIdAndStudyYearSemester(
                        userId, event.getHeldYear(), event.getHeldSemester()
                );
    }

    private void promoteHackathonStatusBySchedule(HackathonEvent event, LocalDateTime now) {
        if (event.getStatus() == HackathonStatus.ENDED) {
            return;
        }

        HackathonStatus scheduledStatus = resolveScheduledStatus(event, now);
        if (isLaterStatus(scheduledStatus, event.getStatus())) {
            event.changeStatus(scheduledStatus);
        }
    }

    private HackathonStatus resolveScheduledStatus(HackathonEvent event, LocalDateTime now) {
        if (!now.isBefore(event.getEndsAt())) {
            return HackathonStatus.JUDGING;
        }
        if (!now.isBefore(event.getStartsAt())) {
            return HackathonStatus.IN_PROGRESS;
        }
        if (event.getTeamBuildingStartsAt() != null && !now.isBefore(event.getTeamBuildingStartsAt())) {
            return HackathonStatus.TEAM_BUILDING;
        }
        return HackathonStatus.RECRUITING;
    }

    private boolean isLaterStatus(HackathonStatus candidate, HackathonStatus current) {
        return STATUS_FLOW.indexOf(candidate) > STATUS_FLOW.indexOf(current);
    }

    private void assertRegistrationOpen(HackathonEvent event, LocalDateTime now) {
        if (event.getRecruitStartsAt() != null && now.isBefore(event.getRecruitStartsAt())) {
            throw new ForifException(ErrorCode.HACKATHON_REGISTRATION_CLOSED);
        }
        if (event.getRecruitEndsAt() != null && !now.isBefore(event.getRecruitEndsAt())) {
            throw new ForifException(ErrorCode.HACKATHON_REGISTRATION_CLOSED);
        }
    }

    private void assertRegisteredParticipant(Long hackathonId, Long userId) {
        HackathonParticipant participant = hackathonRepository.findParticipant(hackathonId, userId)
                .orElseThrow(() -> new ForifException(ErrorCode.HACKATHON_PARTICIPANT_REQUIRED));
        if (participant.getStatus() != ParticipantStatus.REGISTERED) {
            throw new ForifException(ErrorCode.HACKATHON_PARTICIPANT_REQUIRED);
        }
    }

    private void assertNoTeam(Long hackathonId, Long userId) {
        if (hackathonRepository.findTeamMember(hackathonId, userId).isPresent()) {
            throw new ForifException(ErrorCode.HACKATHON_ALREADY_TEAM_MEMBER);
        }
    }

    private void assertTeamLeader(HackathonTeam team, Long userId) {
        if (!team.isLeader(userId)) {
            throw new ForifException(ErrorCode.HACKATHON_TEAM_LEADER_REQUIRED);
        }
    }

    private void assertPendingJoinRequest(HackathonJoinRequest request) {
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new ForifException(ErrorCode.HACKATHON_JOIN_REQUEST_NOT_PENDING);
        }
    }

    private void assertTeamCapacity(HackathonTeam team) {
        if (team.getMaxMembers() == null) {
            return;
        }
        if (hackathonRepository.countTeamMembers(team.getId()) >= team.getMaxMembers()) {
            throw new ForifException(ErrorCode.HACKATHON_TEAM_CAPACITY_EXCEEDED);
        }
    }

    private void assertStatus(HackathonEvent event, HackathonStatus status) {
        if (event.getStatus() != status) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_STATUS);
        }
    }

    private void assertTeamEditableStatus(HackathonEvent event) {
        if (event.getStatus() != HackathonStatus.TEAM_BUILDING
                && event.getStatus() != HackathonStatus.IN_PROGRESS) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_STATUS);
        }
    }

    private void assertNextStatus(HackathonStatus currentStatus, HackathonStatus nextStatus) {
        int current = STATUS_FLOW.indexOf(currentStatus);
        int next = STATUS_FLOW.indexOf(nextStatus);
        if (current < 0 || next != current + 1) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_STATUS);
        }
    }

    private void assertSubmissionOpen(HackathonEvent event) {
        assertStatus(event, HackathonStatus.IN_PROGRESS);
        if (now().isAfter(event.getEndsAt())) {
            throw new ForifException(ErrorCode.HACKATHON_SUBMISSION_CLOSED);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(DateUtils.ZONE_SEOUL);
    }

    private void validatePeriod(LocalDateTime recruitStartsAt, LocalDateTime recruitEndsAt,
                                LocalDateTime teamBuildingStartsAt, LocalDateTime teamBuildingEndsAt,
                                LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_PERIOD);
        }
        if (recruitStartsAt != null && recruitEndsAt != null && !recruitStartsAt.isBefore(recruitEndsAt)) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_PERIOD);
        }
        if (teamBuildingStartsAt != null && teamBuildingEndsAt != null && !teamBuildingStartsAt.isBefore(teamBuildingEndsAt)) {
            throw new ForifException(ErrorCode.HACKATHON_INVALID_PERIOD);
        }
    }

    private boolean matchesSubmissionSearch(HackathonSubmission submission, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        return containsIgnoreCase(submission.getProjectName(), search)
                || containsIgnoreCase(submission.getSummary(), search);
    }

    private boolean matchesTechStack(List<HackathonSubmissionTechStack> techStacks, String techStack) {
        if (techStack == null || techStack.isBlank()) {
            return true;
        }
        String normalizedTechStack = HackathonTechStackPolicy.normalize(techStack);
        return techStacks.stream()
                .map(HackathonSubmissionTechStack::getNormalizedName)
                .anyMatch(normalizedTechStack::equals);
    }

    private Map<Long, Integer> archiveAwardPriorityByTeamId(Long hackathonId) {
        return hackathonRepository.findAwards(hackathonId).stream()
                .collect(Collectors.toMap(
                        award -> award.getTeam().getId(),
                        award -> archiveAwardPriority(award.getAwardName()),
                        Math::min
                ));
    }

    private int archiveAwardPriority(String awardName) {
        if (awardName == null || awardName.isBlank()) {
            return Integer.MAX_VALUE;
        }

        String normalized = awardName.replaceAll("\\s+", "");
        if (normalized.contains("대상")) {
            return 0;
        }
        if (normalized.contains("최우수")) {
            return 1;
        }
        if (normalized.contains("우수")) {
            return 2;
        }
        if (normalized.contains("아이디어톤") && normalized.contains("특별상")) {
            return 3;
        }
        return Integer.MAX_VALUE;
    }

    private String hackathonUploadDirectory(HackathonEvent event) {
        return "hackathons/%d-%d".formatted(event.getHeldYear(), event.getHeldSemester());
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private TeamResponse toTeamResponse(HackathonTeam team) {
        List<TeamMemberResponse> members = hackathonRepository.findTeamMembers(team.getId()).stream()
                .map(TeamMemberResponse::from)
                .toList();
        return TeamResponse.of(team, members.size(), members);
    }

    private SubmissionResponse toSubmissionResponse(HackathonSubmission submission) {
        List<String> techStacks = techStacksBySubmissionId(List.of(submission))
                .getOrDefault(submission.getId(), List.of());
        return toSubmissionResponse(submission, techStacks);
    }

    private SubmissionResponse toSubmissionResponse(HackathonSubmission submission, List<String> techStacks) {
        return SubmissionResponse.of(submission, techStacks, toFileViewUrl(submission.getPresentationFile()));
    }

    private String toFileViewUrl(String objectKey) {
        return FileViewUrls.resolveViewUrl(filePort, objectKey);
    }

    private Map<Long, List<String>> techStacksBySubmissionId(List<HackathonSubmission> submissions) {
        return techStackNamesBySubmissionId(techStackEntitiesBySubmissionId(submissions));
    }

    private Map<Long, List<HackathonSubmissionTechStack>> techStackEntitiesBySubmissionId(
            List<HackathonSubmission> submissions
    ) {
        List<Long> submissionIds = submissions.stream().map(HackathonSubmission::getId).toList();
        return hackathonRepository.findTechStacksBySubmissionIds(submissionIds).stream()
                .collect(Collectors.groupingBy(
                        techStack -> techStack.getSubmission().getId()
                ));
    }

    private Map<Long, List<String>> techStackNamesBySubmissionId(
            Map<Long, List<HackathonSubmissionTechStack>> techStackEntities
    ) {
        return techStackEntities.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(HackathonSubmissionTechStack::getName).toList()
                ));
    }

    private EvaluationResponse toEvaluationResponse(HackathonEvaluation evaluation) {
        List<EvaluationRequest.Score> scores = scoresByEvaluationId(List.of(evaluation))
                .getOrDefault(evaluation.getId(), List.of());
        return EvaluationResponse.of(evaluation, scores);
    }

    private Map<Long, List<EvaluationRequest.Score>> scoresByEvaluationId(List<HackathonEvaluation> evaluations) {
        List<Long> evaluationIds = evaluations.stream().map(HackathonEvaluation::getId).toList();
        return hackathonRepository.findEvaluationScoresByEvaluationIds(evaluationIds).stream()
                .collect(Collectors.groupingBy(
                        score -> score.getEvaluation().getId(),
                        Collectors.mapping(
                                score -> new EvaluationRequest.Score(score.getCriterion().getId(), score.getScore()),
                                Collectors.toList()
                        )
                ));
    }

    private EvaluationSummaryResponse toSummary(Long teamId,
                                                List<HackathonEvaluation> teamEvaluations,
                                                List<HackathonEvaluationScore> scores,
                                                Map<Long, HackathonEvaluation> evaluationById,
                                                Map<Long, HackathonEvaluationCriterion> criteria) {
        BigDecimal sumTotalScore = teamEvaluations.stream()
                .map(HackathonEvaluation::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageTotalScore = sumTotalScore.divide(
                BigDecimal.valueOf(teamEvaluations.size()), 2, RoundingMode.HALF_UP);

        Set<Long> evaluationIds = teamEvaluations.stream()
                .map(HackathonEvaluation::getId)
                .collect(Collectors.toSet());
        Map<Long, List<HackathonEvaluationScore>> scoresByCriterion = scores.stream()
                .filter(score -> evaluationIds.contains(score.getEvaluation().getId()))
                .collect(Collectors.groupingBy(score -> score.getCriterion().getId()));

        List<EvaluationSummaryResponse.CriterionAverage> criterionAverages = scoresByCriterion.entrySet().stream()
                .map(entry -> {
                    BigDecimal sum = entry.getValue().stream()
                            .map(score -> BigDecimal.valueOf(score.getScore()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal average = sum.divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
                    HackathonEvaluationCriterion criterion = criteria.get(entry.getKey());
                    return new EvaluationSummaryResponse.CriterionAverage(
                            entry.getKey(),
                            criterion != null ? criterion.getName() : null,
                            average
                    );
                })
                .sorted(Comparator.comparing(EvaluationSummaryResponse.CriterionAverage::criterionId))
                .toList();

        HackathonEvaluation firstEvaluation = evaluationById.get(teamEvaluations.get(0).getId());
        return new EvaluationSummaryResponse(
                teamId,
                firstEvaluation.getTargetTeam().getName(),
                averageTotalScore,
                sumTotalScore,
                teamEvaluations.size(),
                criterionAverages
        );
    }

    private record ScoreValidationResult(
            Map<Long, HackathonEvaluationCriterion> criteriaById,
            BigDecimal totalScore
    ) {
    }
}
