package org.forif_backend.infrastructure.persistence.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffInterviewRecordRepositoryImpl implements StaffInterviewRecordRepository {

    private final StaffInterviewRecordJpaRepository recordJpaRepository;
    private final StaffInterviewAnswerJpaRepository answerJpaRepository;
    private final StaffInterviewExtraQuestionJpaRepository extraQuestionJpaRepository;

    @Override
    public List<StaffInterviewRecord> findRecordsByRecruitmentId(Integer recruitmentId) {
        return recordJpaRepository.findAllByRecruitmentId(recruitmentId);
    }

    @Override
    public Optional<StaffInterviewRecord> findRecordByApplicationId(Integer applicationId) {
        return recordJpaRepository.findByApplicationId(applicationId);
    }

    @Override
    public StaffInterviewRecord saveRecord(StaffInterviewRecord record) {
        return recordJpaRepository.save(record);
    }

    @Override
    public List<StaffInterviewAnswer> findAnswersByApplicationId(Integer applicationId) {
        return answerJpaRepository.findAllByApplicationId(applicationId);
    }

    @Override
    public Optional<StaffInterviewAnswer> findAnswer(Integer applicationId, Integer questionId) {
        return answerJpaRepository.findOne(applicationId, questionId);
    }

    @Override
    public boolean existsAnswerByQuestionId(Integer questionId) {
        return answerJpaRepository.existsByQuestionId(questionId);
    }

    @Override
    public StaffInterviewAnswer saveAnswer(StaffInterviewAnswer answer) {
        return answerJpaRepository.save(answer);
    }

    @Override
    public void deleteAnswer(StaffInterviewAnswer answer) {
        answerJpaRepository.delete(answer);
    }

    @Override
    public List<StaffInterviewExtraQuestion> findExtraQuestionsByApplicationId(Integer applicationId) {
        return extraQuestionJpaRepository.findAllByApplicationId(applicationId);
    }

    @Override
    public Optional<StaffInterviewExtraQuestion> findExtraQuestionById(Integer extraQuestionId) {
        return extraQuestionJpaRepository.findById(extraQuestionId);
    }

    @Override
    public StaffInterviewExtraQuestion saveExtraQuestion(StaffInterviewExtraQuestion extraQuestion) {
        return extraQuestionJpaRepository.save(extraQuestion);
    }

    @Override
    public void deleteExtraQuestion(StaffInterviewExtraQuestion extraQuestion) {
        extraQuestionJpaRepository.delete(extraQuestion);
    }

    @Override
    public void flush() {
        recordJpaRepository.flush();
    }
}
