package org.forif_backend.infrastructure.persistence.study;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudyRepositoryImpl implements StudyRepository {
    private final StudyJpaRepository studyJpaRepository;
    private final StudyTagJpaRepository studyTagJpaRepository;
    private final StudyApplyJpaRepository studyApplyJpaRepository;
    private final StudyApplyReferenceJpaRepository studyApplyReferenceJpaRepository;
    private final StudyApplyPlanJpaRepository studyApplyPlanJpaRepository;

    @Override
    public Optional<Study> findStudyById(Integer studyId) {
        return studyJpaRepository.findById(studyId);
    }

    @Override
    public List<StudyTag> findAllStudyTagById(List<Long> tagIds) {
        return studyTagJpaRepository.findAllById(tagIds);
    }

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


}
