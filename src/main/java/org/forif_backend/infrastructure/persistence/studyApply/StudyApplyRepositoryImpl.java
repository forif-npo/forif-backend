package org.forif_backend.infrastructure.persistence.studyApply;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.studyApply.StudyApply;
import org.forif_backend.domain.studyApply.StudyApplyPlan;
import org.forif_backend.domain.studyApply.StudyApplyReference;
import org.forif_backend.domain.studyApply.StudyApplyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StudyApplyRepositoryImpl implements StudyApplyRepository {
    private final StudyApplyJpaRepository studyApplyJpaRepository;
    private final StudyApplyReferenceJpaRepository studyApplyReferenceJpaRepository;
    private final StudyApplyPlanJpaRepository studyApplyPlanJpaRepository;

    @Override
    public void saveStudyApply(StudyApply studyApply) {
        studyApplyJpaRepository.save(studyApply);
    }

    @Override
    public void saveAllStudyApplyPlan(List<StudyApplyPlan> studyApplyPlans) {
        studyApplyPlanJpaRepository.saveAll(studyApplyPlans);
    }

    @Override
    public void saveAllStudyApplyReference(List<StudyApplyReference> studyApplyReferences) {
        studyApplyReferenceJpaRepository.saveAll(studyApplyReferences);
    }

    @Override
    public List<StudyApply> findAllStudyApplyByMentorId(Long mentorId) {
        return studyApplyJpaRepository.findByMentorId(mentorId);
    }
}
