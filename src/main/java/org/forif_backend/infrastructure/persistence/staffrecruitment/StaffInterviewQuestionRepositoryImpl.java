package org.forif_backend.infrastructure.persistence.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.StaffInterviewQuestion;
import org.forif_backend.domain.staffrecruitment.StaffInterviewQuestionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffInterviewQuestionRepositoryImpl implements StaffInterviewQuestionRepository {

    private final StaffInterviewQuestionJpaRepository jpaRepository;

    @Override
    public List<StaffInterviewQuestion> findAllByRecruitmentId(Integer recruitmentId) {
        return jpaRepository.findAllByRecruitmentId(recruitmentId);
    }

    @Override
    public Optional<StaffInterviewQuestion> findById(Integer questionId) {
        return jpaRepository.findById(questionId);
    }

    @Override
    public StaffInterviewQuestion save(StaffInterviewQuestion question) {
        return jpaRepository.save(question);
    }

    @Override
    public void delete(StaffInterviewQuestion question) {
        jpaRepository.delete(question);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }
}
