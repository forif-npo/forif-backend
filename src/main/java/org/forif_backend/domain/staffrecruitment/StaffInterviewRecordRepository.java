package org.forif_backend.domain.staffrecruitment;

import java.util.List;
import java.util.Optional;

/** 면접 진행 기록 저장소. 상태·질문 답변·기타 질문을 함께 다룬다. */
public interface StaffInterviewRecordRepository {

    List<StaffInterviewRecord> findRecordsByRecruitmentId(Integer recruitmentId);

    Optional<StaffInterviewRecord> findRecordByApplicationId(Integer applicationId);

    StaffInterviewRecord saveRecord(StaffInterviewRecord record);

    List<StaffInterviewAnswer> findAnswersByApplicationId(Integer applicationId);

    Optional<StaffInterviewAnswer> findAnswer(Integer applicationId, Integer questionId);

    boolean existsAnswerByQuestionId(Integer questionId);

    StaffInterviewAnswer saveAnswer(StaffInterviewAnswer answer);

    void deleteAnswer(StaffInterviewAnswer answer);

    List<StaffInterviewExtraQuestion> findExtraQuestionsByApplicationId(Integer applicationId);

    Optional<StaffInterviewExtraQuestion> findExtraQuestionById(Integer extraQuestionId);

    StaffInterviewExtraQuestion saveExtraQuestion(StaffInterviewExtraQuestion extraQuestion);

    void deleteExtraQuestion(StaffInterviewExtraQuestion extraQuestion);

    void flush();
}
