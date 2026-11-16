package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.file.TransactionalFileCleanup;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationDetail;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 운영진 지원서 제출·조회.
 *
 * 자격·기간·문항 검증을 한 트랜잭션에서 수행하고, 중복 제출은 DB 유니크 제약으로 최종 방어한다.
 * 지원자 식별은 인증 컨텍스트의 userId만 사용하며 요청 본문의 사용자 값을 믿지 않는다.
 */
@Service
@RequiredArgsConstructor
public class StaffApplicationService {

    private final StaffApplicationRepository staffApplicationRepository;
    private final StaffRecruitmentRepository staffRecruitmentRepository;
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

        addChoices(application, recruitment, command.departmentChoices());
        addAnswers(application, recruitment, command.answers(), files);

        StaffApplication saved = staffApplicationRepository.save(application);
        try {
            staffApplicationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 같은 회원의 동시 제출은 (recruitment_id, applicant_user_id) 유니크 제약에서 걸린다.
            throw new ForifException(ErrorCode.STAFF_APPLICATION_ALREADY_EXISTS);
        }
        return StaffApplicationDetail.from(saved);
    }

    /** 본인 지원서. 다른 사람의 지원서는 ID를 알아도 이 경로로 볼 수 없다. */
    @Transactional(readOnly = true)
    public StaffApplicationDetail getMine(Integer recruitmentId, Long userId) {
        return staffApplicationRepository.findDetailByRecruitmentIdAndApplicantId(recruitmentId, userId)
                .map(StaffApplicationDetail::from)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_APPLICATION_NOT_FOUND));
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
        SemesterInfo active = semesterService.getActive();
        boolean activeMember = studyUserRepository.existsByUserIdAndStudyYearSemester(
                userId, active.actYear(), active.actSemester());
        if (!activeMember) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_ELIGIBILITY_REQUIRED);
        }
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

    private void addChoices(StaffApplication application, StaffRecruitment recruitment,
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
        created.forEach(application::addChoice);
    }

    private void addAnswers(StaffApplication application, StaffRecruitment recruitment,
                            List<SubmitStaffApplicationCommand.AnswerCommand> answers,
                            Map<Integer, MultipartFile> files) {
        Map<Integer, StaffRecruitmentQuestion> questions = recruitment.getQuestions().stream()
                .collect(Collectors.toMap(StaffRecruitmentQuestion::getId, Function.identity()));

        if (files == null || files.keySet().stream().anyMatch(id -> !questions.containsKey(id)
                || questions.get(id).getType() != StaffRecruitmentQuestionType.FILE)) {
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
            if (question.getType() == StaffRecruitmentQuestionType.FILE) {
                MultipartFile file = files.get(question.getId());
                if (value != null && !value.isBlank()) {
                    throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
                }
                if (file != null && !file.isEmpty()) {
                    validateAttachment(file);
                    value = filePort.uploadFile(file, PRIVATE_FILE_DIRECTORY + "/" + recruitment.getId());
                    TransactionalFileCleanup.deleteOnRollback(filePort, value, "운영진 지원서");
                }
            } else if (files.containsKey(question.getId())) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
            if (value != null && value.length() > question.getMaxLength()) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
            if (question.getType() == StaffRecruitmentQuestionType.LINK && value != null && !value.isBlank()) {
                value = value.trim();
                validateLink(value);
            }
            if (question.isRequired() && (value == null || value.isBlank())) {
                throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
            }
            created.add(StaffApplicationAnswer.create(question, value));
        }

        boolean missingRequired = questions.values().stream()
                .anyMatch(question -> question.isRequired() && !answered.contains(question.getId()));
        if (missingRequired || !answered.containsAll(files.keySet())) {
            throw new ForifException(ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
        }
        created.forEach(application::addAnswer);
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
