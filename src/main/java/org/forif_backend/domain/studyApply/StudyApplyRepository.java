package org.forif_backend.domain.studyApply;

import java.util.List;

public interface StudyApplyRepository {
    void saveStudyApply(StudyApply studyApply);
    void saveAllStudyApplyPlan(List<StudyApplyPlan> studyApplyPlans);
    void saveAllStudyApplyReference(List<StudyApplyReference> studyApplyReferences);
}
