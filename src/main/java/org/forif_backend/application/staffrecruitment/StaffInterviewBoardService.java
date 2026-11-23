package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewBoardDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewBoardDetail.Card;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail.Slot;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewSheetDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewSheetDetail.DepartmentQuestions;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewSheetDetail.ExtraQuestion;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewSheetDetail.QuestionAnswer;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 면접 관리 보드와 면접 기록.
 *
 * 회장단이 지원자 카드를 면접 전 → 면접 완료 → 합격·불합격으로 옮기고,
 * 면접 중에 공통·부서 질문의 답변과 면접자별 기타 질문을 적는다.
 * 보드는 끌어 옮기는 화면이라 상태 사이의 이동을 막지 않는다.
 * 기록을 바꾸는 작업은 모집이 마감된 회차에서만 한다.
 */
@Service
@RequiredArgsConstructor
public class StaffInterviewBoardService {

    private final StaffRecruitmentRepository staffRecruitmentRepository;
    private final StaffApplicationRepository staffApplicationRepository;
    private final StaffInterviewPlanRepository staffInterviewPlanRepository;
    private final StaffInterviewScheduleRepository staffInterviewScheduleRepository;
    private final StaffInterviewQuestionRepository staffInterviewQuestionRepository;
    private final StaffInterviewRecordRepository staffInterviewRecordRepository;

    @Transactional(readOnly = true)
    public StaffInterviewBoardDetail getBoard(Integer recruitmentId) {
        requireRecruitment(recruitmentId);

        Map<Integer, Slot> assigned = assignedSlots(recruitmentId);
        Map<Integer, StaffInterviewStatus> statuses = staffInterviewRecordRepository
                .findRecordsByRecruitmentId(recruitmentId).stream()
                .collect(Collectors.toMap(record -> record.getApplication().getId(), StaffInterviewRecord::getStatus));

        List<Card> cards = staffApplicationRepository.findAllWithChoicesByRecruitmentId(recruitmentId).stream()
                .map(application -> new Card(
                        application.getId(),
                        application.getApplicantName(),
                        assigned.get(application.getId()),
                        departmentName(application, StaffApplicationChoice.FIRST_PRIORITY),
                        departmentName(application, StaffApplicationChoice.SECOND_PRIORITY),
                        statuses.getOrDefault(application.getId(), StaffInterviewStatus.BEFORE)))
                // 같은 칸 안에서는 면접 시각이 빠른 순, 시각이 없으면 맨 뒤
                .sorted(Comparator.comparing(Card::assignedSlot,
                        Comparator.nullsLast(Comparator.comparing(Slot::date).thenComparingInt(Slot::startMinute))))
                .toList();

        return new StaffInterviewBoardDetail(slotMinutes(recruitmentId), cards);
    }

    @Transactional(readOnly = true)
    public StaffInterviewSheetDetail getSheet(Integer recruitmentId, Integer applicationId) {
        requireRecruitment(recruitmentId);
        StaffApplication application = requireApplication(recruitmentId, applicationId);
        return sheet(recruitmentId, application);
    }

    @Transactional
    public StaffInterviewSheetDetail changeStatus(Integer recruitmentId, Integer applicationId,
                                                  StaffInterviewStatus status) {
        requireClosed(recruitmentId);
        StaffApplication application = requireApplication(recruitmentId, applicationId);
        if (status == null) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        staffInterviewRecordRepository.findRecordByApplicationId(applicationId)
                .ifPresentOrElse(
                        record -> record.changeStatus(status),
                        () -> staffInterviewRecordRepository.saveRecord(
                                StaffInterviewRecord.create(application, status)));
        staffInterviewRecordRepository.flush();
        return sheet(recruitmentId, application);
    }

    /**
     * 공통·부서 질문의 답변을 적는다. 빈 답변을 보내면 지운다.
     * 그 지원자의 1·2지망이 아닌 부서 질문에는 적을 수 없다.
     */
    @Transactional
    public StaffInterviewSheetDetail saveAnswer(Integer recruitmentId, Integer applicationId,
                                                Integer questionId, String answer) {
        requireClosed(recruitmentId);
        StaffApplication application = requireApplication(recruitmentId, applicationId);
        StaffInterviewQuestion question = staffInterviewQuestionRepository.findById(questionId)
                .filter(candidate -> candidate.getRecruitment().getId().equals(recruitmentId))
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_INTERVIEW_QUESTION_NOT_FOUND));
        if (!question.isCommon() && !chosenDepartmentIds(application).contains(question.getDepartment().getId())) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        Optional<StaffInterviewAnswer> current = staffInterviewRecordRepository.findAnswer(applicationId, questionId);
        if (answer == null || answer.isBlank()) {
            current.ifPresent(staffInterviewRecordRepository::deleteAnswer);
        } else {
            current.ifPresentOrElse(
                    saved -> saved.updateAnswer(answer),
                    () -> staffInterviewRecordRepository.saveAnswer(
                            StaffInterviewAnswer.create(application, question, answer)));
        }
        staffInterviewRecordRepository.flush();
        return sheet(recruitmentId, application);
    }

    @Transactional
    public StaffInterviewSheetDetail addExtraQuestion(Integer recruitmentId, Integer applicationId, String content) {
        requireClosed(recruitmentId);
        StaffApplication application = requireApplication(recruitmentId, applicationId);
        int nextOrder = staffInterviewRecordRepository.findExtraQuestionsByApplicationId(applicationId).stream()
                .mapToInt(StaffInterviewExtraQuestion::getDisplayOrder)
                .max()
                .orElse(0) + 1;
        staffInterviewRecordRepository.saveExtraQuestion(
                StaffInterviewExtraQuestion.create(application, content, nextOrder));
        staffInterviewRecordRepository.flush();
        return sheet(recruitmentId, application);
    }

    /** 내용과 답변 중 보낸 것만 바꾼다. */
    @Transactional
    public StaffInterviewSheetDetail updateExtraQuestion(Integer recruitmentId, Integer applicationId,
                                                         Integer extraQuestionId, String content, String answer) {
        requireClosed(recruitmentId);
        StaffApplication application = requireApplication(recruitmentId, applicationId);
        StaffInterviewExtraQuestion extra = requireExtra(applicationId, extraQuestionId);
        if (content != null) {
            extra.updateContent(content);
        }
        if (answer != null) {
            extra.updateAnswer(answer);
        }
        staffInterviewRecordRepository.flush();
        return sheet(recruitmentId, application);
    }

    @Transactional
    public StaffInterviewSheetDetail deleteExtraQuestion(Integer recruitmentId, Integer applicationId,
                                                         Integer extraQuestionId) {
        requireClosed(recruitmentId);
        StaffApplication application = requireApplication(recruitmentId, applicationId);
        staffInterviewRecordRepository.deleteExtraQuestion(requireExtra(applicationId, extraQuestionId));
        staffInterviewRecordRepository.flush();
        return sheet(recruitmentId, application);
    }

    private StaffInterviewSheetDetail sheet(Integer recruitmentId, StaffApplication application) {
        Integer applicationId = application.getId();
        Map<Integer, String> answers = staffInterviewRecordRepository.findAnswersByApplicationId(applicationId)
                .stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), StaffInterviewAnswer::getAnswer));
        List<StaffInterviewQuestion> questions = staffInterviewQuestionRepository.findAllByRecruitmentId(recruitmentId);

        List<QuestionAnswer> common = questions.stream()
                .filter(StaffInterviewQuestion::isCommon)
                .map(question -> new QuestionAnswer(question.getId(), question.getContent(), answers.get(question.getId())))
                .toList();

        StaffInterviewStatus status = staffInterviewRecordRepository.findRecordByApplicationId(applicationId)
                .map(StaffInterviewRecord::getStatus)
                .orElse(StaffInterviewStatus.BEFORE);
        List<ExtraQuestion> extras = staffInterviewRecordRepository.findExtraQuestionsByApplicationId(applicationId)
                .stream()
                .map(extra -> new ExtraQuestion(extra.getId(), extra.getContent(), extra.getAnswer()))
                .toList();

        return new StaffInterviewSheetDetail(
                applicationId,
                application.getApplicantName(),
                status,
                assignedSlots(recruitmentId).get(applicationId),
                slotMinutes(recruitmentId),
                common,
                departmentQuestions(application, StaffApplicationChoice.FIRST_PRIORITY, questions, answers),
                departmentQuestions(application, StaffApplicationChoice.SECOND_PRIORITY, questions, answers),
                extras);
    }

    private DepartmentQuestions departmentQuestions(StaffApplication application, int priority,
                                                    List<StaffInterviewQuestion> questions,
                                                    Map<Integer, String> answers) {
        return application.getChoices().stream()
                .filter(choice -> choice.getPriority() == priority)
                .findFirst()
                .map(choice -> new DepartmentQuestions(
                        choice.getDepartment().getId(),
                        choice.getDepartment().getName(),
                        questions.stream()
                                .filter(question -> !question.isCommon()
                                        && question.getDepartment().getId().equals(choice.getDepartment().getId()))
                                .map(question -> new QuestionAnswer(
                                        question.getId(), question.getContent(), answers.get(question.getId())))
                                .toList()))
                .orElse(null);
    }

    private Set<Integer> chosenDepartmentIds(StaffApplication application) {
        return application.getChoices().stream()
                .map(choice -> choice.getDepartment().getId())
                .collect(Collectors.toSet());
    }

    private String departmentName(StaffApplication application, int priority) {
        return application.getChoices().stream()
                .filter(choice -> choice.getPriority() == priority)
                .findFirst()
                .map(choice -> choice.getDepartment().getName())
                .orElse(null);
    }

    private Map<Integer, Slot> assignedSlots(Integer recruitmentId) {
        return staffInterviewScheduleRepository.findAssignments(recruitmentId).stream()
                .collect(Collectors.toMap(
                        assignment -> assignment.getApplication().getId(),
                        assignment -> new Slot(assignment.getSlotDate(), assignment.getStartMinute())));
    }

    private Integer slotMinutes(Integer recruitmentId) {
        return staffInterviewPlanRepository.findByRecruitmentId(recruitmentId)
                .map(StaffInterviewPlan::getSlotMinutes)
                .orElse(null);
    }

    private StaffRecruitment requireRecruitment(Integer recruitmentId) {
        return staffRecruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
    }

    private void requireClosed(Integer recruitmentId) {
        if (requireRecruitment(recruitmentId).getStatus() != StaffRecruitmentStatus.CLOSED) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        }
    }

    private StaffApplication requireApplication(Integer recruitmentId, Integer applicationId) {
        return staffApplicationRepository.findDetailById(applicationId)
                .filter(application -> application.getRecruitment().getId().equals(recruitmentId))
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_APPLICATION_NOT_FOUND));
    }

    private StaffInterviewExtraQuestion requireExtra(Integer applicationId, Integer extraQuestionId) {
        return staffInterviewRecordRepository.findExtraQuestionById(extraQuestionId)
                .filter(extra -> extra.getApplication().getId().equals(applicationId))
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_INTERVIEW_EXTRA_QUESTION_NOT_FOUND));
    }
}
