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
    private final StudyPlanJpaRepository studyPlanJpaRepository;
    private final StudyReferenceJpaRepository studyReferenceJpaRepository;

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
    public List<Study> findStudiesByUserId(Long userId) {
        return studyQueryRepository.findStudiesByUserId(userId);
    }

    @Override
    public Optional<Study> findStudyByIdWithTags(Integer studyId) {
        return studyJpaRepository.findByIdWithTags(studyId);
    }

    @Override
    public void saveStudy(Study study) {
        studyJpaRepository.save(study);
    }

    @Override
    public void saveAllStudyPlan(List<StudyPlan> plans) {
        studyPlanJpaRepository.saveAll(plans);
    }

    @Override
    public void saveAllStudyReference(List<StudyReference> references) {
        studyReferenceJpaRepository.saveAll(references);
    }

    @Override
    public List<Study> findAllStudiesByMentorIdAndIsApplied(Long mentorId, Boolean isApplied) {
        return studyQueryRepository.findAllStudiesByMentorIdAndIsApplied(mentorId, isApplied);
    }
}
