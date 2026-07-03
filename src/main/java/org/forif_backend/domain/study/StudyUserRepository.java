package org.forif_backend.domain.study;

import java.util.List;
import java.util.Optional;

public interface StudyUserRepository {
    Optional<StudyUser> findByUserIdAndStudyId(Long userId, Integer studyId);

    List<StudyUser> findAllByStudyId(Integer studyId);

    void save(StudyUser studyUser);

    void deleteByUserIdAndStudyId(Long userId, Integer studyId);

    boolean existsByUserIdAndStudyYearSemester(Long userId, int year, int semester);
}
