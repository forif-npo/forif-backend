package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewQuestionDetail;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 면접 질문 관리. 회장단이 공통 질문과 부서별 질문을 써 두면 모든 면접자 화면에 똑같이 나온다.
 */
@Service
@RequiredArgsConstructor
public class StaffInterviewQuestionService {

    private final StaffRecruitmentRepository staffRecruitmentRepository;
    private final StaffInterviewQuestionRepository staffInterviewQuestionRepository;

    @Transactional(readOnly = true)
    public List<StaffInterviewQuestionDetail> getQuestions(Integer recruitmentId) {
        requireRecruitment(recruitmentId);
        return staffInterviewQuestionRepository.findAllByRecruitmentId(recruitmentId).stream()
                .map(StaffInterviewQuestionDetail::from)
                .toList();
    }

    /** 같은 묶음(공통 또는 같은 부서)의 맨 뒤에 붙인다. */
    @Transactional
    public StaffInterviewQuestionDetail create(Integer recruitmentId, Integer departmentId, String content) {
        StaffRecruitment recruitment = staffRecruitmentRepository.findDetailById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
        StaffRecruitmentDepartment department = departmentId == null ? null
                : recruitment.getDepartments().stream()
                        .filter(candidate -> candidate.getId().equals(departmentId))
                        .findFirst()
                        .orElseThrow(() -> new ForifException(ErrorCode.INVALID_INPUT));

        int nextOrder = staffInterviewQuestionRepository.findAllByRecruitmentId(recruitmentId).stream()
                .filter(question -> Objects.equals(
                        question.isCommon() ? null : question.getDepartment().getId(), departmentId))
                .mapToInt(StaffInterviewQuestion::getDisplayOrder)
                .max()
                .orElse(0) + 1;

        StaffInterviewQuestion saved = staffInterviewQuestionRepository.save(
                StaffInterviewQuestion.create(recruitment, department, content, nextOrder));
        staffInterviewQuestionRepository.flush();
        return StaffInterviewQuestionDetail.from(saved);
    }

    @Transactional
    public StaffInterviewQuestionDetail updateContent(Integer recruitmentId, Integer questionId, String content) {
        StaffInterviewQuestion question = requireQuestion(recruitmentId, questionId);
        question.updateContent(content);
        staffInterviewQuestionRepository.flush();
        return StaffInterviewQuestionDetail.from(question);
    }

    @Transactional
    public void delete(Integer recruitmentId, Integer questionId) {
        staffInterviewQuestionRepository.delete(requireQuestion(recruitmentId, questionId));
    }

    private void requireRecruitment(Integer recruitmentId) {
        staffRecruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
    }

    private StaffInterviewQuestion requireQuestion(Integer recruitmentId, Integer questionId) {
        return staffInterviewQuestionRepository.findById(questionId)
                .filter(question -> question.getRecruitment().getId().equals(recruitmentId))
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_INTERVIEW_QUESTION_NOT_FOUND));
    }
}
