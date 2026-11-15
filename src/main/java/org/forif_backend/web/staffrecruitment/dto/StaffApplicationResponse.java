package org.forif_backend.web.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffApplicationDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationSummary;
import org.forif_backend.domain.staffrecruitment.StaffApplicationStatus;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;

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

    /** 본인·회장단 상세 응답 */
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
            List<AnswerResponse> answers
    ) {
        public static DetailResponse from(StaffApplicationDetail detail) {
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
                                    answer.answer()))
                            .toList());
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
