package org.forif_backend.domain.study;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StudyRepository {
    Optional<Study> findStudyById(Integer studyId);
    List<StudyTag> findAllStudyTagById(List<Long> tagIds);
    List<Study> getStudies(StudySearchCond cond, Integer cursor, int size);
    List<Study> getStudiesWithOffset(StudySearchCond cond, int page, int size);
    long countStudiesForUser(StudySearchCond cond);
    List<Study> findStudiesByUserId(Long userId);

    /**
     * 여러 사용자의 현재 학기 스터디명을 배치 조회
     */
    Map<Long, String> findCurrentStudyNamesByUserIds(List<Long> userIds, int year, int semester);

    /**
     * 스터디 ID로 스터디 정보 조회 (태그 정보 포함)
     * @param studyId 스터디 ID
     * @return 스터디 정보 (태그 포함)
     */
    Optional<Study> findStudyByIdWithTags(Integer studyId);

    /**
     * 여러 스터디 ID로 스터디 정보 배치 조회 (태그 정보 포함)
     */
    Map<Integer, Study> findStudiesByIdsWithTags(List<Integer> studyIds);

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
    List<Study> findAllStudiesByMentorId(Long mentorId);

    /**
     * 커서 기반 스터디 목록 조회 (Admin용)
     */
    List<Study> searchStudiesWithCursor(Integer cursor, int size, Integer year, Integer semester, String search, List<StudyStatus> studyStatuses);
    List<Study> searchAdminStudiesWithOffset(int page, int size, Integer year, Integer semester, String search, List<StudyStatus> studyStatuses);

    /**
     * 조건에 맞는 스터디 총 건수
     */
    long countStudies(Integer year, Integer semester, String search, List<StudyStatus> studyStatuses);

    /**
     * 스터디 ID 목록에 해당하는 멘티 수 조회
     */
    Map<Integer, Long> countMenteesByStudyIds(List<Integer> studyIds);

    /**
     * 스터디 ID로 스터디 플랜 목록 조회
     */
    List<StudyPlan> findStudyPlansByStudyId(Integer studyId);

    /**
     * 스터디 ID로 스터디 참고자료 목록 조회
     */
    List<StudyReference> findStudyReferencesByStudyId(Integer studyId);

    /**
     * 스터디 ID로 멘토-스터디 매핑 목록 조회
     */
    List<MentorStudy> findMentorStudiesByStudyId(Integer studyId);

    /**
     * 스터디 삭제
     */
    void deleteStudyById(Integer studyId);

    /**
     * 스터디 ID에 해당하는 커리큘럼 전체 삭제
     */
    void deleteStudyPlansByStudyId(Integer studyId);

    /**
     * 스터디 ID에 해당하는 참고자료 전체 삭제
     */
    void deleteStudyReferencesByStudyId(Integer studyId);

    /**
     * 스터디 ID에 해당하는 수강생 전체 삭제
     */
    void deleteStudyUsersByStudyId(Integer studyId);

    /**
     * 스터디 ID에 해당하는 멘토-스터디 매핑 전체 삭제
     */
    void deleteMentorStudiesByStudyId(Integer studyId);

    /**
     * 멘토 아이디로 스터디와 태그들을 함께 조회한다.
     *
     * @param mentorId 멘토 아이디
     * @return 스터디 리스트
     */
    List<Study> findStudiesByMentorId(Long mentorId);
}
