package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail.Slot;
import org.forif_backend.domain.staffrecruitment.StaffInterviewStatus;

import java.util.List;

/**
 * 면접자 한 명의 면접 기록지. 지원서 내용은 기존 지원서 상세 API로 따로 읽는다.
 * 부서 질문은 그 지원자의 1지망·2지망 부서 것만 담는다.
 */
public record StaffInterviewSheetDetail(
        Integer applicationId,
        String applicantName,
        StaffInterviewStatus status,
        Slot assignedSlot,
        Integer slotMinutes,
        List<QuestionAnswer> commonQuestions,
        DepartmentQuestions firstDepartment,
        DepartmentQuestions secondDepartment,
        List<ExtraQuestion> extraQuestions
) {

    public record QuestionAnswer(Integer questionId, String content, String answer) {
    }

    public record DepartmentQuestions(Integer departmentId, String departmentName, List<QuestionAnswer> questions) {
    }

    public record ExtraQuestion(Integer extraQuestionId, String content, String answer) {
    }
}
