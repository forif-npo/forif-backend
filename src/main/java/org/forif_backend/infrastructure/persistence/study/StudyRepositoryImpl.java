package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.*;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudySearchCond;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyRepositoryImpl implements StudyRepository {
    private final StudyJpaRepository studyJpaRepository;
    private final StudyQueryRepository studyQueryRepository;
    private final StudyTagJpaRepository studyTagJpaRepository;
    private final StudyApplyJpaRepository studyApplyJpaRepository;
    private final StudyApplyReferenceJpaRepository studyApplyReferenceJpaRepository;
    private final StudyApplyPlanJpaRepository studyApplyPlanJpaRepository;

    @Override
    public Optional<Study> findStudyById(Integer studyId) {
        return studyJpaRepository.findById(studyId);
    }

    @Override
    public List<Study> getStudies(StudySearchCond cond, Long offset, Long limit) {
        return studyQueryRepository.searchStudies(cond, offset, limit);
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
