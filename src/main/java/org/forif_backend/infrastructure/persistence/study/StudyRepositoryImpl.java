package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.*;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudySearchCond;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.domain.user.User;
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
    public boolean existsByActYearAndActSemesterAndAutonomousFlagTrue(int actYear, int actSemester) {
        return studyJpaRepository.existsByActYearAndActSemesterAndAutonomousFlagTrue(actYear, actSemester);
    }

    @Override
    public Optional<Study> findAutonomousStudyByYearSemester(int actYear, int actSemester) {
        return studyJpaRepository.findByActYearAndActSemesterAndAutonomousFlagTrue(actYear, actSemester);
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
    public List<StudyTag> findAllStudyTagByName(List<String> tagNames) {
        return studyTagJpaRepository.findByNameInIgnoreCase(tagNames);
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
    public Map<Long, String> findMentorStudyNamesByUserIds(
            List<Long> userIds,
            Integer year,
            Integer semester
    ) {
        return studyQueryRepository.findMentorStudyNamesByUserIds(userIds, year, semester);
    }

    @Override
    public List<User> searchMentors(Long cursor, int size, String search) {
        return studyQueryRepository.searchMentors(cursor, size, search);
    }

    @Override
    public List<User> searchMentorsWithOffset(int page, int size, String search, List<SortCriteria> sorting) {
        return studyQueryRepository.searchMentorsWithOffset(page, size, search, sorting);
    }

    @Override
    public long countMentors(String search) {
        return studyQueryRepository.countMentors(search);
    }

    @Override
    public List<User> searchMentorsByYearSemester(
            int year,
            int semester,
            Long cursor,
            int size,
            String search
    ) {
        return studyQueryRepository.searchMentorsByYearSemester(year, semester, cursor, size, search);
    }

    @Override
    public List<User> searchMentorsByYearSemesterWithOffset(
            int year,
            int semester,
            int page,
            int size,
            String search,
            List<SortCriteria> sorting
    ) {
        return studyQueryRepository.searchMentorsByYearSemesterWithOffset(year, semester, page, size, search, sorting);
    }

    @Override
    public long countMentorsByYearSemester(int year, int semester, String search) {
        return studyQueryRepository.countMentorsByYearSemester(year, semester, search);
    }

    @Override
    public Map<Long, List<Study>> findCurrentStudiesByUserIds(List<Long> userIds, int year, int semester) {
        return studyQueryRepository.findCurrentStudiesByUserIds(userIds, year, semester);
    }

    @Override
    public Map<Long, List<Study>> findCurrentMentorStudiesByUserIds(List<Long> userIds, int year, int semester) {
        return studyQueryRepository.findCurrentMentorStudiesByUserIds(userIds, year, semester);
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
    public int updateRecruitStatusForApprovedStudies(int actYear, int actSemester, RecruitStatus recruitStatus) {
        return studyJpaRepository.updateRecruitStatusForApprovedStudies(
                actYear, actSemester, recruitStatus, StudyStatus.APPROVED);
    }

    @Override
    public int closeRecruitmentForNonActiveStudies(int activeYear, int activeSemester) {
        return studyJpaRepository.closeRecruitmentForNonActiveStudies(
                activeYear, activeSemester, RecruitStatus.CLOSED,
                List.of(StudyStatus.APPROVED, StudyStatus.STARTED));
    }

    @Override
    public int startApprovedStudies(int actYear, int actSemester) {
        return studyJpaRepository.startApprovedStudies(
                actYear, actSemester, StudyStatus.APPROVED, StudyStatus.STARTED);
    }

    @Override
    public int startPastApprovedStudies(int activeYear, int activeSemester) {
        return studyJpaRepository.startPastApprovedStudies(
                activeYear, activeSemester, StudyStatus.APPROVED, StudyStatus.STARTED);
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
    public List<Study> searchAdminStudiesWithOffset(int page, int size, Integer year, Integer semester, String search, List<StudyStatus> studyStatuses, List<SortCriteria> sorting) {
        return studyQueryRepository.searchAdminStudiesWithOffset(page, size, year, semester, search, studyStatuses, sorting);
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
        // FOR-116 이후 tb_mentor_study에는 아무것도 쌓이지 않는다. 멘토는 tb_study의 FK가 원본이다.
        return !studyQueryRepository.findMentorUserIdsByUserIds(List.of(mentorId), year, semester).isEmpty();
    }

    @Override
    public java.util.Set<Long> findMentorUserIdsByUserIds(java.util.List<Long> userIds, int year, int semester) {
        return studyQueryRepository.findMentorUserIdsByUserIds(userIds, year, semester);
    }

    @Override
    public void deleteStudyById(Integer studyId) {
        studyJpaRepository.deleteById(studyId);
    }

    @Override
    public void deleteStudyPlansByStudyId(Integer studyId) {
        studyPlanJpaRepository.deleteByStudyId(studyId);
        studyPlanJpaRepository.flush();
    }

    @Override
    public void deleteStudyReferencesByStudyId(Integer studyId) {
        studyReferenceJpaRepository.deleteByStudyId(studyId);
        studyReferenceJpaRepository.flush();
    }

    @Override
    public void deleteStudyReferencesByIds(List<UUID> referenceIds) {
        if (referenceIds.isEmpty()) {
            return;
        }
        studyReferenceJpaRepository.deleteByIdIn(referenceIds);
        studyReferenceJpaRepository.flush();
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

    @Override
    public List<Study> findMentorHistoryByMentorId(Long mentorId) {
        return studyQueryRepository.findMentorHistoryByMentorId(mentorId);
    }

    @Override
    public List<Study> findStudyApplicationsByMentorId(Long mentorId, int actYear, int actSemester) {
        return studyQueryRepository.findStudyApplicationsByMentorId(mentorId, actYear, actSemester);
    }

}
