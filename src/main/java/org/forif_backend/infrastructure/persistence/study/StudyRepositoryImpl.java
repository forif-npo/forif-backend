package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
    public List<Study> getStudies(StudySearchCond cond, Integer cursor, int size) {
        return studyQueryRepository.searchStudies(cond, cursor, size);
    }

    @Override
    public List<Study> getStudiesWithOffset(StudySearchCond cond, int page, int size) {
        return studyQueryRepository.searchStudiesWithOffset(cond, page, size);
    }

    @Override
    public long countStudiesForUser(StudySearchCond cond) {
        return studyQueryRepository.countStudiesForUser(cond);
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
    public Map<Long, String> findCurrentStudyNamesByUserIds(List<Long> userIds, int year, int semester) {
        return studyQueryRepository.findCurrentStudyNamesByUserIds(userIds, year, semester);
    }

    @Override
    public Optional<Study> findStudyByIdWithTags(Integer studyId) {
        return studyJpaRepository.findByIdWithTags(studyId);
    }

    @Override
    public Map<Integer, Study> findStudiesByIdsWithTags(List<Integer> studyIds) {
        if (studyIds == null || studyIds.isEmpty()) {
            return Map.of();
        }
        return studyJpaRepository.findByIdsWithTags(studyIds).stream()
                .collect(Collectors.toMap(Study::getId, s -> s));
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
    public List<Study> searchStudiesWithCursor(Integer cursor, int size, Integer year, Integer semester, String search, List<StudyStatus> studyStatuses) {
        return studyQueryRepository.searchStudiesWithCursor(cursor, size, year, semester, search, studyStatuses);
    }

    @Override
    public List<Study> searchAdminStudiesWithOffset(int page, int size, Integer year, Integer semester, String search, List<StudyStatus> studyStatuses) {
        return studyQueryRepository.searchAdminStudiesWithOffset(page, size, year, semester, search, studyStatuses);
    }

    @Override
    public long countStudies(Integer year, Integer semester, String search, List<StudyStatus> studyStatuses) {
        return studyQueryRepository.countStudies(year, semester, search, studyStatuses);
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
    public boolean existsMentorStudyByMentorIdAndStudyYearSemester(Long mentorId, int year, int semester) {
        return mentorStudyJpaRepository.existsByMentorIdAndStudyActYearAndStudyActSemester(
                mentorId,
                year,
                semester
        );
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
