package org.forif_backend.domain.staffrecruitment;

import java.util.List;
import java.util.Optional;

public interface StaffInterviewQuestionRepository {

    /** 공통 질문이 먼저, 부서 질문은 부서별로 순서대로 나온다. */
    List<StaffInterviewQuestion> findAllByRecruitmentId(Integer recruitmentId);

    Optional<StaffInterviewQuestion> findById(Integer questionId);

    StaffInterviewQuestion save(StaffInterviewQuestion question);

    void delete(StaffInterviewQuestion question);

    void flush();
}
