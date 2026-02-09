package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Map;
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
    private final MentorStudyJpaRepository mentorStudyJpaRepository;
    private final StudyUserJpaRepository studyUserJpaRepository;

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
    public List<Study> findAllStudiesByMentorId(Long mentorId) {
        return studyQueryRepository.findAllStudiesByMentorId(mentorId);
    }

    @Override
    public List<Study> searchStudiesWithCursor(Integer cursor, int size, Integer year, Integer semester, String search) {
        return studyQueryRepository.searchStudiesWithCursor(cursor, size, year, semester, search);
    }

    @Override
    public long countStudies(Integer year, Integer semester, String search) {
        return studyQueryRepository.countStudies(year, semester, search);
    }

    @Override
    public Map<Integer, Long> countMenteesByStudyIds(List<Integer> studyIds) {
        return studyQueryRepository.countMenteesByStudyIds(studyIds);
    }

    @Override
    public List<StudyPlan> findStudyPlansByStudyId(Integer studyId) {
        return studyPlanJpaRepository.findByStudyId(studyId);
    }

    @Override
    public List<StudyReference> findStudyReferencesByStudyId(Integer studyId) {
        return studyReferenceJpaRepository.findByStudyId(studyId);
    }

    @Override
    public List<MentorStudy> findMentorStudiesByStudyId(Integer studyId) {
        return mentorStudyJpaRepository.findByStudyId(studyId);
    }

    @Override
    public void deleteStudyById(Integer studyId) {
        studyJpaRepository.deleteById(studyId);
    }

    @Override
    public void deleteStudyPlansByStudyId(Integer studyId) {
        studyPlanJpaRepository.deleteByStudyId(studyId);
    }

    @Override
    public void deleteStudyReferencesByStudyId(Integer studyId) {
        studyReferenceJpaRepository.deleteByStudyId(studyId);
    }

    @Override
    public void deleteStudyUsersByStudyId(Integer studyId) {
        studyUserJpaRepository.deleteByStudyId(studyId);
    }

    @Override
    public void deleteMentorStudiesByStudyId(Integer studyId) {
        mentorStudyJpaRepository.deleteByStudyId(studyId);
    }

    @Override
    public List<Study> findStudiesByMentorId(Long mentorId){
        return studyQueryRepository.findStudiesByMentorId(mentorId);
    }

}
