package org.forif_backend.domain.study;

import java.util.List;
import java.util.Optional;

public interface StudyRepository {
    Optional<Study> findStudyById(Integer studyId);
    List<StudyTag> findAllStudyTagById(List<Long> tagIds);
    void saveStudyApply(StudyApply studyApply);
    void saveAllStudyApplyPlan(List<StudyApplyPlan> studyApplyPlans);
    void saveAllStudyApplyReference(List<StudyApplyReference> studyApplyReferences);
}
