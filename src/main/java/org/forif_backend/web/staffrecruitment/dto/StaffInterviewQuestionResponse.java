package org.forif_backend.web.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewQuestionDetail;

/** 면접 질문. departmentId가 null이면 공통 질문이다. */
public record StaffInterviewQuestionResponse(
        Integer questionId,
        Integer departmentId,
        String departmentName,
        String content,
        int displayOrder
) {

    public static StaffInterviewQuestionResponse from(StaffInterviewQuestionDetail detail) {
        return new StaffInterviewQuestionResponse(detail.questionId(), detail.departmentId(),
                detail.departmentName(), detail.content(), detail.displayOrder());
    }
}
