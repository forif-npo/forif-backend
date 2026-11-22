package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffInterviewQuestion;

/** 면접 질문. departmentId가 null이면 공통 질문이다. */
public record StaffInterviewQuestionDetail(
        Integer questionId,
        Integer departmentId,
        String departmentName,
        String content,
        int displayOrder
) {

    public static StaffInterviewQuestionDetail from(StaffInterviewQuestion question) {
        return new StaffInterviewQuestionDetail(
                question.getId(),
                question.isCommon() ? null : question.getDepartment().getId(),
                question.isCommon() ? null : question.getDepartment().getName(),
                question.getContent(),
                question.getDisplayOrder());
    }
}
