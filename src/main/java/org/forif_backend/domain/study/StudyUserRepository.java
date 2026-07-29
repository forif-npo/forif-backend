package org.forif_backend.domain.study;

import java.util.List;
import java.util.Optional;

public interface StudyUserRepository {
    Optional<StudyUser> findByUserIdAndStudyId(Long userId, Integer studyId);

    List<StudyUser> findAllByStudyId(Integer studyId);

    List<StudyUser> findAllByUserId(Long userId);

    void save(StudyUser studyUser);

    void deleteByUserIdAndStudyId(Long userId, Integer studyId);

    boolean existsByUserIdAndStudyYearSemester(Long userId, int year, int semester);

    /** 해당 학기 수강생 수 (수료증 발급 현황 안내용) */
    long countBySemester(int year, int semester);

    /** 해당 학기 수료증 발급 완료 수 */
    long countIssuedCertificatesBySemester(int year, int semester);
}
