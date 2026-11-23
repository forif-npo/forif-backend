package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.file.TransactionalFileCleanup;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationInterviewSummary;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationSummary;
import org.forif_backend.application.staffrecruitment.dto.MyStaffApplicationSummary;
import org.forif_backend.application.staffrecruitment.dto.SubmitStaffApplicationCommand;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationAnswer;
import org.forif_backend.domain.staffrecruitment.StaffApplicationChoice;
import org.forif_backend.domain.staffrecruitment.StaffApplicationRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentDepartment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestion;
import org.forif_backend.domain.staffrecruitment.StaffApplicationInterviewSlot;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlan;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlanRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewScheduleRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewRecordRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 운영진 지원서 제출·수정·취소·조회.
 *
 * 자격·기간·문항 검증을 한 트랜잭션에서 수행하고, 중복 제출은 DB 유니크 제약으로 최종 방어한다.
 * 지원자 식별은 인증 컨텍스트의 userId만 사용하며 요청 본문의 사용자 값을 믿지 않는다.
 */
@Service
@RequiredArgsConstructor
public class StaffApplicationService {

    private final StaffApplicationRepository staffApplicationRepository;
    private final StaffRecruitmentRepository staffRecruitmentRepository;
    private final StaffInterviewPlanRepository staffInterviewPlanRepository;
    private final StaffInterviewScheduleRepository staffInterviewScheduleRepository;
    private final StaffInterviewRecordRepository staffInterviewRecordRepository;
    private final UserRepository userRepository;
    private final StudyUserRepository studyUserRepository;
    private final SemesterService semesterService;
    private final FilePort filePort;
    private static final long MAX_ATTACHMENT_BYTES = 5L * 1024 * 1024;
    private static final String PRIVATE_FILE_DIRECTORY = "staff-applications";

    // ==================== 지원자 ====================

    @Transactional
    public StaffApplicationDetail submit(Integer recruitmentId, Long userId, SubmitStaffApplicationCommand command) {
        return submit(recruitmentId, userId, command, Map.of());
    }

    @Transactional
    public StaffApplicationDetail submit(Integer recruitmentId, Long userId, SubmitStaffApplicationCommand command,
                                         Map<Integer, MultipartFile> files) {
        StaffRecruitment recruitment = staffRecruitmentRepository.findDetailById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
        if (!recruitment.isPubliclyVisible()) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND);
        }

        requireActiveMember(userId);
        if (staffApplicationRepository.existsByRecruitmentIdAndApplicantId(recruitmentId, userId)) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_ALREADY_EXISTS);
        }

        User applicant = userRepository.findById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        StaffApplication application = StaffApplication.submit(
                recruitment,
                applicant,
                applicant.getUserName(),
                applicant.getEmail(),
                applicant.getPhoneNum(),
                departmentNameOf(applicant),
                LocalDateTime.now());

        createChoices(recruitment, command.departmentChoices()).forEach(application::addChoice);
        createAnswers(recruitment, command.answers(), files, Map.of(), Set.of())
                .forEach(application::addAnswer);
        createInterviewSlots(recruitmentId, command.interviewSlots()).forEach(application::addInterviewSlot);

        StaffApplication saved = staffApplicationRepository.save(application);
        try {
            staffApplicationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 같은 회원의 동시 제출은 (recruitment_id, applicant_user_id) 유니크 제약에서 걸린다.
            throw new ForifException(ErrorCode.STAFF_APPLICATION_ALREADY_EXISTS);
        }
        return StaffApplicationDetail.from(saved);
    }

    /** 접수 중인 본인 지원서만 수정한다. 새 파일은 롤백 시, 교체·제거한 파일은 커밋 후 정리한다. */
    @Transactional
    public StaffApplicationDetail update(Integer recruitmentId, Long userId,
                                         SubmitStaffApplicationCommand command,
                                         Map<Integer, MultipartFile> files, Set<Integer> removedFileQuestionIds) {
        StaffApplication application = requireMutableMine(recruitmentId, userId);
        requireActiveMember(userId);
        StaffRecruitment recruitment = application.getRecruitment();
        List<StaffApplicationChoice> choices = createChoices(recruitment, command.departmentChoices());
        Set<Integer> currentQuestionIds = recruitment.getQuestions().stream()
                .map(StaffRecruitmentQuestion::getId).collect(Collectors.toSet());
        if (command.answers() == null || command.answers().stream().anyMatch(answer -> answer == null
                || answer.questionId() == null)
                || !command.answers().stream().map(SubmitStaffApplicationCommand.AnswerCommand::questionId)
                .collect(Collectors.toSet()).equals(currentQuestionIds)
                || command.answers().size() != currentQuestionIds.size()) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        }
        Map<Integer, String> existingFiles = application.getAnswers().stream()
                .filter(answer -> answer.getQuestionType() == StaffRecruitmentQuestionType.FILE
                        && answer.getAnswer() != null && !answer.getAnswer().isBlank())
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), StaffApplicationAnswer::getAnswer));
        List<StaffApplicationAnswer> answers = createAnswers(
                recruitment, command.answers(), files, existingFiles, removedFileQuestionIds);

        Set<String> retainedFiles = answers.stream()
                .filter(answer -> answer.getQuestionType() == StaffRecruitmentQuestionType.FILE)
                .map(StaffApplicationAnswer::getAnswer)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
        List<String> replacedFiles = existingFiles.values().stream()
                .filter(value -> value != null && !value.isBlank() && !retainedFiles.contains(value))
                .toList();

        List<StaffApplicationInterviewSlot> interviewSlots =
                createInterviewSlots(recruitmentId, command.interviewSlots());

        application.clearSubmission();
        // 지망·답변·면접 시간 모두 유니크 제약이 있어, 지우고 다시 넣기 전에 삭제를 먼저 반영한다.
        staffApplicationRepository.flush();
        choices.forEach(application::addChoice);
        answers.forEach(application::addAnswer);
        interviewSlots.forEach(application::addInterviewSlot);
        TransactionalFileCleanup.deleteAfterCommit(filePort, replacedFiles, "운영진 지원서 수정");
        return StaffApplicationDetail.from(application);
    }

    /** 접수 중인 본인 지원서를 삭제해 같은 회차에 다시 지원할 수 있게 한다. */
    @Transactional
    public void cancel(Integer recruitmentId, Long userId) {
        StaffApplication application = requireMutableMine(recruitmentId, userId);
        List<String> storedFiles = application.getAnswers().stream()
                .filter(answer -> answer.getQuestionType() == StaffRecruitmentQuestionType.FILE)
                .map(StaffApplicationAnswer::getAnswer)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        staffApplicationRepository.delete(application);
        TransactionalFileCleanup.deleteAfterCommit(filePort, storedFiles, "운영진 지원서 취소");
    }

    private StaffApplication requireMutableMine(Integer recruitmentId, Long userId) {
        StaffApplication application = staffApplicationRepository
                .findForMutation(recruitmentId, userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_APPLICATION_NOT_FOUND));
        if (!application.getRecruitment().isApplicationOpen(LocalDateTime.now())) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_OPEN);
        }
        return application;
    }

    /** 본인 지원서. 다른 사람의 지원서는 ID를 알아도 이 경로로 볼 수 없다. */
    @Transactional(readOnly = true)
    public StaffApplicationDetail getMine(Integer recruitmentId, Long userId) {
        return staffApplicationRepository.findDetailByRecruitmentIdAndApplicantId(recruitmentId, userId)
                .map(StaffApplicationDetail::from)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_APPLICATION_NOT_FOUND));
    }

    /** getMine으로 본인 소유를 확인한 지원서의 면접 공개 정보만 조회한다. */
    @Transactional(readOnly = true)
    public StaffApplicationInterviewSummary getInterviewSummary(StaffApplicationDetail application) {
        var assignment = staffInterviewScheduleRepository.findAssignmentByApplicationId(application.applicationId());
        Integer slotMinutes = assignment.flatMap(ignored ->
                staffInterviewPlanRepository.findByRecruitmentId(application.recruitmentId()))
                .map(StaffInterviewPlan::getSlotMinutes)
                .orElse(null);
        var status = staffInterviewRecordRepository.findRecordByApplicationId(application.applicationId())
                .map(record -> record.getStatus())
                .orElse(null);
        return new StaffApplicationInterviewSummary(
                assignment.map(a -> a.getSlotDate()).orElse(null),
                assignment.map(a -> a.getStartMinute()).orElse(null),
                slotMinutes,
                status);
    }

    @Transactional(readOnly = true)
    public boolean canModifyMine(Integer recruitmentId, Long userId) {
        return staffRecruitmentRepository.findById(recruitmentId)
                .filter(recruitment -> recruitment.isApplicationOpen(LocalDateTime.now()))
                .isPresent() && isActiveMember(userId);
    }

    @Transactional(readOnly = true)
    public List<MyStaffApplicationSummary> getMyHistory(Long userId) {
        return staffApplicationRepository.findByApplicantId(userId).stream()
                .map(MyStaffApplicationSummary::from)
                .toList();
    }

    // ==================== 회장단 ====================

    @Transactional(readOnly = true)
    public CursorPageResponse<StaffApplicationSummary> getAdminList(Integer recruitmentId, Integer departmentId,
                                                                    String search, Integer cursor, Integer page,
                                                                    int size) {
        requireRecruitment(recruitmentId);
        String keyword = (search == null || search.isBlank()) ? null : search.trim();
        long totalElements = staffApplicationRepository.countForAdmin(recruitmentId, departmentId, keyword);

        CursorPageResponse<StaffApplication> applications = CursorPageResponse.paginate(
                page, size, totalElements,
                () -> staffApplicationRepository.findForAdminWithOffset(recruitmentId, departmentId, keyword,
                        page == null ? 0 : page, size),
                () -> staffApplicationRepository.findForAdminWithCursor(recruitmentId, departmentId, keyword,
                        cursor, size),
                StaffApplication::getId);

        return applications.withContent(
                applications.content().stream().map(StaffApplicationSummary::from).toList());
    }

    @Transactional(readOnly = true)
    public StaffApplicationDetail getAdminDetail(Integer recruitmentId, Integer applicationId) {
        StaffApplication application = staffApplicationRepository.findDetailById(applicationId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_APPLICATION_NOT_FOUND));
        // 다른 회차의 지원서를 이 경로로 열람하지 못하게 한다.
        if (!application.getRecruitment().getId().equals(recruitmentId)) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_NOT_FOUND);
        }
        return StaffApplicationDetail.from(application);
    }

    /** 호출자가 본인 또는 열람 권한을 검사한 뒤에만 이 메서드에 접근한다. */
    public Attachment downloadAttachment(StaffApplicationDetail detail, Integer questionId) {
        StaffApplicationDetail.AnswerInfo answer = detail.answers().stream()
                .filter(item -> item.questionId().equals(questionId)
                        && item.questionType() == StaffRecruitmentQuestionType.FILE)
                .findFirst()
                .orElseThrow(() -> new ForifException(ErrorCode.FILE_NOT_FOUND));
        String objectKey = answer.answer();
        if (objectKey == null || !objectKey.startsWith(PRIVATE_FILE_DIRECTORY + "/")) {
            throw new ForifException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            return new Attachment(filePort.downloadBytes(objectKey), attachmentName(objectKey));
        } catch (ForifException e) {
            if (e.getErrorCode() == ErrorCode.INVALID_FILE_ATTACHMENT) {
                throw new ForifException(ErrorCode.FILE_NOT_FOUND);
            }
            throw e;
        }
    }

    public record Attachment(byte[] bytes, String filename) {}

    public static String attachmentName(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return "";
        String basename = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        return basename.replaceFirst("^[0-9a-fA-F-]{36}-", "");
    }

    // ==================== 내부 ====================

    /**
     * 지원 자격은 현재 활동 학기의 수강 관계로 판정한다.
     * 회비 납부 플래그만으로 판정하지 않는다. 등록 철회 시 해당 학기 관계가 삭제되기 때문이다.
     */
    private void requireActiveMember(Long userId) {
        if (!isActiveMember(userId)) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_ELIGIBILITY_REQUIRED);
        }
    }

    private boolean isActiveMember(Long userId) {
        SemesterInfo active = semesterService.getActive();
        return studyUserRepository.existsByUserIdAndStudyYearSemester(
                userId, active.actYear(), active.actSemester());
    }

    private void requireRecruitment(Integer recruitmentId) {
        if (staffRecruitmentRepository.findById(recruitmentId).isEmpty()) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND);
        }
    }

    private String departmentNameOf(User applicant) {
        if (applicant.getDepartmentEntity() != null) {
            return applicant.getDepartmentEntity().getDepartmentName();
        }
        return applicant.getDepartment();
    }

    /**
     * 면접 가능 시간을 검증해 만든다.
     *
     * 면접 일정이 설정된 회차에서만 받는다. 설정되지 않았는데 값을 보내면 거절하고,
     * 설정된 범위 밖의 칸도 거절한다. 같은 칸을 두 번 보내면 한 번만 저장한다.
     */
    private List<StaffApplicationInterviewSlot> createInterviewSlots(
            Integer recruitmentId, List<SubmitStaffApplicationCommand.InterviewSlotCommand> commands) {
        List<SubmitStaffApplicationCommand.InterviewSlotCommand> requested =
                commands == null ? List.of() : commands;
        StaffInterviewPlan plan = staffInterviewPlanRepository.findByRecruitmentId(recruitmentId).orElse(null);

        if (plan == null) {
            if (!requested.isEmpty()) {
                throw new ForifException(ErrorCode.STAFF_INTERVIEW_SLOT_INVALID);
            }
            return List.of();
        }

        Set<SubmitStaffApplicationCommand.InterviewSlotCommand> unique = new LinkedHashSet<>(requested);
        unique.forEach(slot -> {
            if (slot.date() == null || !plan.covers(slot.date(), slot.startMinute())) {
                throw new ForifException(ErrorCode.STAFF_INTERVIEW_SLOT_INVALID);
            }
        });
        if (plan.isRequired() && unique.isEmpty()) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_SLOT_INVALID);
        }

        return unique.stream()
                .map(slot -> StaffApplicationInterviewSlot.create(slot.date(), slot.startMinute()))
                .toList();
    }

    private List<StaffApplicationChoice> createChoices(StaffRecruitment recruitment,
                                                       List<SubmitStaffApplicationCommand.ChoiceCommand> choices) {
        if (choices == null || choices.isEmpty()) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
        }

        Map<Integer, StaffRecruitmentDepartment> departments = recruitment.getDepartments().stream()
                .filter(StaffRecruitmentDepartment::isActive)
                .collect(Collectors.toMap(StaffRecruitmentDepartment::getId, Function.identity()));

        Set<Integer> usedPriorities = new HashSet<>();
        Set<Integer> usedDepartments = new HashSet<>();
        List<StaffApplicationChoice> created = new ArrayList<>();

        for (SubmitStaffApplicationCommand.ChoiceCommand choice : choices) {
            if (choice.priority() != StaffApplicationChoice.FIRST_PRIORITY
                    && choice.priority() != StaffApplicationChoice.SECOND_PRIORITY) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
            }
            if (!usedPriorities.add(choice.priority()) || !usedDepartments.add(choice.departmentId())) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
            }
            StaffRecruitmentDepartment department = departments.get(choice.departmentId());
            if (department == null) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
            }
            created.add(StaffApplicationChoice.create(department, choice.priority()));
        }

        if (!usedPriorities.contains(StaffApplicationChoice.FIRST_PRIORITY)) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_CHOICE);
        }
        return created;
    }

    private List<StaffApplicationAnswer> createAnswers(StaffRecruitment recruitment,
                                                       List<SubmitStaffApplicationCommand.AnswerCommand> answers,
                                                       Map<Integer, MultipartFile> files,
                                                       Map<Integer, String> existingFiles,
                                                       Set<Integer> removedFileQuestionIds) {
        Map<Integer, StaffRecruitmentQuestion> questions = recruitment.getQuestions().stream()
                .collect(Collectors.toMap(StaffRecruitmentQuestion::getId, Function.identity()));

        if (files == null || files.keySet().stream().anyMatch(id -> !questions.containsKey(id)
                || !acceptsFile(questions.get(id)))
                || removedFileQuestionIds == null || removedFileQuestionIds.stream().anyMatch(id ->
                !questions.containsKey(id) || !acceptsFile(questions.get(id))
                        || files.containsKey(id))) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        }

        Set<Integer> answered = new HashSet<>();
        List<StaffApplicationAnswer> created = new ArrayList<>();

        for (SubmitStaffApplicationCommand.AnswerCommand answer : (answers == null ? List.<SubmitStaffApplicationCommand.AnswerCommand>of() : answers)) {
            StaffRecruitmentQuestion question = questions.get(answer.questionId());
            if (question == null || !answered.add(answer.questionId())) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
            String value = answer.answer();
            StaffRecruitmentQuestionType answerType = question.getType();
            if (acceptsFile(question)) {
                MultipartFile file = files.get(question.getId());
                if (value != null && !value.isBlank()
                        && (file != null || question.getType() == StaffRecruitmentQuestionType.FILE)) {
                    throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
                }
                if (file != null && file.isEmpty()) {
                    throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
                }
                if (file != null) {
                    validateAttachment(file);
                    value = filePort.uploadFile(file, PRIVATE_FILE_DIRECTORY + "/" + recruitment.getId());
                    TransactionalFileCleanup.deleteOnRollback(filePort, value, "운영진 지원서");
                    answerType = StaffRecruitmentQuestionType.FILE;
                } else if (question.getType() == StaffRecruitmentQuestionType.ATTACHMENT
                        && value != null && !value.isBlank()) {
                    answerType = StaffRecruitmentQuestionType.LINK;
                } else if (!removedFileQuestionIds.contains(question.getId())) {
                    value = existingFiles.get(question.getId());
                    if (value != null && !value.isBlank()) answerType = StaffRecruitmentQuestionType.FILE;
                }
            } else if (files.containsKey(question.getId())) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
            if (answerType != StaffRecruitmentQuestionType.FILE
                    && value != null && value.length() > question.getMaxLength()) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
            if (answerType == StaffRecruitmentQuestionType.LINK && value != null && !value.isBlank()) {
                value = value.trim();
                validateLink(value);
            }
            if (question.isRequired() && (value == null || value.isBlank())) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
            created.add(question.getType() == StaffRecruitmentQuestionType.ATTACHMENT
                    ? StaffApplicationAnswer.createAttachment(question, value, answerType)
                    : StaffApplicationAnswer.create(question, value));
        }

        boolean missingRequired = questions.values().stream()
                .anyMatch(question -> question.isRequired() && !answered.contains(question.getId()));
        if (missingRequired || !answered.containsAll(files.keySet())) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        }
        if (created.stream().filter(answer -> answer.getQuestionType() == StaffRecruitmentQuestionType.FILE
                && answer.getAnswer() != null && !answer.getAnswer().isBlank()).count() > 1) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        }
        return created;
    }

    private boolean acceptsFile(StaffRecruitmentQuestion question) {
        return question.getType() == StaffRecruitmentQuestionType.FILE
                || question.getType() == StaffRecruitmentQuestionType.ATTACHMENT;
    }

    private void validateAttachment(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (file.getSize() > MAX_ATTACHMENT_BYTES || name == null || name.isBlank()
                || name.length() > 200 || name.contains("/") || name.contains("\\")
                || name.chars().anyMatch(character -> character < 32 || "<>:\"|?*".indexOf(character) >= 0)) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
    }

    private void validateLink(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        }
    }
}
