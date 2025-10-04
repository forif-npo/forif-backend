package org.forif_backend.domain.study;

import java.util.Optional;

public interface StudyRepository {
    Optional<Study> findStudyById(Integer studyId);
}
