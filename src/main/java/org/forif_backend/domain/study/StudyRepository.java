package org.forif_backend.domain.study;

import org.forif_backend.domain.studyApply.StudyApply;
import org.forif_backend.domain.studyApply.StudyApplyPlan;
import org.forif_backend.domain.studyApply.StudyApplyReference;

import java.util.List;
import java.util.Optional;

public interface StudyRepository {
    Optional<Study> findStudyById(Integer studyId);
    List<StudyTag> findAllStudyTagById(List<Long> tagIds);
    List<Study> getStudies(StudySearchCond cond, Long offset, Long limit);

    /**
     * 스터디 ID로 스터디 정보 조회 (태그 정보 포함)
     * @param studyId 스터디 ID
     * @return 스터디 정보 (태그 포함)
     */
    Optional<Study> findStudyByIdWithTags(Integer studyId);
}
