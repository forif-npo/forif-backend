package org.forif_backend.web.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffApplicationDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationInterviewSummary;
import org.forif_backend.application.staffrecruitment.StaffApplicationService;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationSummary;
import org.forif_backend.application.staffrecruitment.dto.MyStaffApplicationSummary;
import org.forif_backend.domain.staffrecruitment.StaffApplicationStatus;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffInterviewStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 지원서 응답 모음.
 * 제출 직후 응답은 최소 필드만 내려주고, 상세는 본인과 회장단만 볼 수 있다.
 */
public final class StaffApplicationResponse {

    private StaffApplicationResponse() {
    }

    public record AppliedDepartmentResponse(
            Integer departmentId,
            String name,
            int priority
    ) {
        static AppliedDepartmentResponse from(StaffApplicationDetail.AppliedDepartment department) {
            return new AppliedDepartmentResponse(department.departmentId(), department.name(), department.priority());
        }
    }

    public record AnswerResponse(
            Integer questionId,
            String questionLabel,
            StaffRecruitmentQuestionType questionType,
            String answer
    ) {
    }

    /** 제출 성공 응답 */
    public record SubmitResponse(
            Integer applicationId,
            Integer recruitmentId,
            StaffApplicationStatus status,
            OffsetDateTime submittedAt
    ) {
        public static SubmitResponse from(StaffApplicationDetail detail) {
            return new SubmitResponse(
                    detail.applicationId(),
                    detail.recruitmentId(),
                    detail.status(),
                    StaffRecruitmentResponse.toOffset(detail.submittedAt()));
        }
    }

    /** 본인의 회차별 지원 이력. 상세 답변과 개인정보는 포함하지 않는다. */
    public record MySummaryResponse(
            Integer applicationId,
            Integer recruitmentId,
            int actYear,
            int actSemester,
            StaffApplicationStatus status,
            OffsetDateTime submittedAt
    ) {
        public static MySummaryResponse from(MyStaffApplicationSummary summary) {
            return new MySummaryResponse(
                    summary.applicationId(),
                    summary.recruitmentId(),
                    summary.actYear(),
                    summary.actSemester(),
                    summary.status(),
                    StaffRecruitmentResponse.toOffset(summary.submittedAt()));
        }
    }

    /** 본인·회장단 상세 응답 */
    /** 지원자가 고른 면접 가능 시간 한 칸. startMinute는 자정부터의 분이다. */
    public record InterviewSlotResponse(
            LocalDate date,
            int startMinute
    ) {
    }

    public record InterviewSummaryResponse(
            InterviewSlotResponse assignedSlot,
            Integer slotMinutes,
            StaffInterviewStatus status
    ) {
        static InterviewSummaryResponse from(StaffApplicationInterviewSummary summary) {
            return new InterviewSummaryResponse(
                    summary.date() == null || summary.startMinute() == null
                            ? null : new InterviewSlotResponse(summary.date(), summary.startMinute()),
                    summary.slotMinutes(),
                    summary.status());
        }
    }

    public record DetailResponse(
            Integer applicationId,
            Integer recruitmentId,
            Long applicantUserId,
            String applicantName,
            String applicantEmail,
            String applicantPhoneNum,
            String applicantDepartment,
            List<AppliedDepartmentResponse> appliedDepartments,
            StaffApplicationStatus status,
            OffsetDateTime submittedAt,
            List<AnswerResponse> answers,
            List<InterviewSlotResponse> interviewSlots,
            boolean canModify,
            InterviewSummaryResponse interview
    ) {
        public static DetailResponse from(StaffApplicationDetail detail) {
            return from(detail, false);
        }

        public static DetailResponse from(StaffApplicationDetail detail, boolean canModify) {
            return from(detail, canModify, null);
        }

        public static DetailResponse from(StaffApplicationDetail detail, boolean canModify,
                                          StaffApplicationInterviewSummary interview) {
            return new DetailResponse(
                    detail.applicationId(),
                    detail.recruitmentId(),
                    detail.applicantUserId(),
                    detail.applicantName(),
                    detail.applicantEmail(),
                    detail.applicantPhoneNum(),
                    detail.applicantDepartment(),
                    detail.appliedDepartments().stream().map(AppliedDepartmentResponse::from).toList(),
                    detail.status(),
                    StaffRecruitmentResponse.toOffset(detail.submittedAt()),
                    detail.answers().stream()
                            .map(answer -> new AnswerResponse(
                                    answer.questionId(),
                                    answer.questionLabel(),
                                    answer.questionType(),
                                    answer.questionType() == StaffRecruitmentQuestionType.FILE
                                            ? StaffApplicationService.attachmentName(answer.answer())
                                            : answer.answer()))
                            .toList(),
                    detail.interviewSlots().stream()
                            .map(slot -> new InterviewSlotResponse(slot.date(), slot.startMinute()))
                            .toList(),
                    canModify,
                    interview == null ? null : InterviewSummaryResponse.from(interview));
        }
    }

    /** 회장단 목록 행. 연락처와 답변은 포함하지 않는다. */
    public record SummaryResponse(
            Integer applicationId,
            String applicantName,
            String applicantDepartment,
            List<AppliedDepartmentResponse> appliedDepartments,
            StaffApplicationStatus status,
            OffsetDateTime submittedAt
    ) {
        public static SummaryResponse from(StaffApplicationSummary summary) {
            return new SummaryResponse(
                    summary.applicationId(),
                    summary.applicantName(),
                    summary.applicantDepartment(),
                    summary.appliedDepartments().stream().map(AppliedDepartmentResponse::from).toList(),
                    summary.status(),
                    StaffRecruitmentResponse.toOffset(summary.submittedAt()));
        }
    }
}
