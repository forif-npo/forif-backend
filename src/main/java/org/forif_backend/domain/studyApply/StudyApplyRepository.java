package org.forif_backend.domain.studyApply;

import java.util.List;

public interface StudyApplyRepository {
    void saveStudyApply(StudyApply studyApply);
    void saveAllStudyApplyPlan(List<StudyApplyPlan> studyApplyPlans);
    void saveAllStudyApplyReference(List<StudyApplyReference> studyApplyReferences);

    /**
     * 멘토 ID로 스터디 개설 신청 목록 조회 (Primary 또는 Secondary Mentor)
     * @param mentorId 멘토 ID
     * @return 스터디 개설 신청 목록 (최신순 정렬: createdAt DESC)
     */
    List<StudyApply> findAllStudyApplyByMentorId(Long mentorId);
}
