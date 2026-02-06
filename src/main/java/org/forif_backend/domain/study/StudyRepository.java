package org.forif_backend.domain.study;

import java.util.List;
import java.util.Optional;

public interface StudyRepository {
    Optional<Study> findStudyById(Integer studyId);
    List<StudyTag> findAllStudyTagById(List<Long> tagIds);
    List<Study> getStudies(StudySearchCond cond, Long offset, Long limit);
    List<Study> findStudiesByUserId(Long userId);

    /**
     * 스터디 ID로 스터디 정보 조회 (태그 정보 포함)
     * @param studyId 스터디 ID
     * @return 스터디 정보 (태그 포함)
     */
    Optional<Study> findStudyByIdWithTags(Integer studyId);

    /**
     * 스터디 저장
     */
    void saveStudy(Study study);

    /**
     * 스터디 플랜 일괄 저장
     */
    void saveAllStudyPlan(List<StudyPlan> plans);

    /**
     * 스터디 참고자료 일괄 저장
     */
    void saveAllStudyReference(List<StudyReference> references);

    /**
     * 멘토 ID로 스터디 신청 목록 조회
     */
    List<Study> findAllStudiesByMentorIdAndIsApplied(Long mentorId, Boolean isApplied);

    /**
     * 커서 기반 스터디 목록 조회 (Admin용)
     */
    List<Study> searchStudiesWithCursor(Integer cursor, int size, Integer year, Integer semester, String search);

    /**
     * 조건에 맞는 스터디 총 건수
     */
    long countStudies(Integer year, Integer semester, String search);
}
