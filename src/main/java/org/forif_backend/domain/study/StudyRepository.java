package org.forif_backend.domain.study;

import java.util.List;
import java.util.Optional;

public interface StudyRepository {
    Optional<Study> findStudyById(Integer studyId);
    List<Study> getStudies(StudySearchCond cond, Long offset, Long limit);
}
