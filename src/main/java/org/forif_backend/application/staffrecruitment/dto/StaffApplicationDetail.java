package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationStatus;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 지원서 상세 조회 모델. 본인 조회와 회장단 조회가 같은 모델을 쓴다.
 * 제출 당시 snapshot을 그대로 보여 주며 회원 프로필의 현재 값으로 바꾸지 않는다.
 */
public record StaffApplicationDetail(
        Integer applicationId,
        Integer recruitmentId,
        Long applicantUserId,
        String applicantName,
        String applicantEmail,
        String applicantPhoneNum,
        String applicantDepartment,
        List<AppliedDepartment> appliedDepartments,
        StaffApplicationStatus status,
        LocalDateTime submittedAt,
        List<AnswerInfo> answers
) {

    public record AppliedDepartment(
            Integer departmentId,
            String name,
            int priority
    ) {
    }

    public record AnswerInfo(
            Integer questionId,
            String questionLabel,
            StaffRecruitmentQuestionType questionType,
            String answer
    ) {
    }

    public static StaffApplicationDetail from(StaffApplication application) {
        return new StaffApplicationDetail(
                application.getId(),
                application.getRecruitment().getId(),
                application.getApplicant() == null ? null : application.getApplicant().getId(),
                application.getApplicantName(),
                application.getApplicantEmail(),
                application.getApplicantPhoneNum(),
                application.getApplicantDepartment(),
                application.getChoices().stream()
                        .map(choice -> new AppliedDepartment(
                                choice.getDepartment().getId(),
                                choice.getDepartment().getName(),
                                choice.getPriority()))
                        .toList(),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getAnswers().stream()
                        .map(answer -> new AnswerInfo(
                                answer.getQuestion().getId(),
                                answer.getQuestionLabel(),
                                answer.getQuestionType(),
                                answer.getAnswer()))
                        .toList()
        );
    }
}
