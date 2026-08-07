package org.forif_backend.domain.study;

import java.util.List;
import java.util.Optional;

import org.forif_backend.domain.user.User;

public interface StudyUserRepository {
    Optional<StudyUser> findByUserIdAndStudyId(Long userId, Integer studyId);

    List<StudyUser> findAllByStudyId(Integer studyId);

    List<StudyUser> findAllByUserId(Long userId);

    List<User> findUsersByYearSemester(int year, int semester, String search);

    void save(StudyUser studyUser);

    void deleteByUserIdAndStudyId(Long userId, Integer studyId);

    /** 현재 활동 학기에서 부원을 제외할 때, 해당 학기의 모든 수강 관계를 삭제한다. */
    int deleteByUserIdAndStudyYearSemester(Long userId, int year, int semester);

    boolean existsByUserIdAndStudyYearSemester(Long userId, int year, int semester);

    /** 해당 학기 수강생 수 (수료증 발급 현황 안내용) */
    long countBySemester(int year, int semester);

    /** 해당 학기 수료증 발급 완료 수 */
    long countIssuedCertificatesBySemester(int year, int semester);
}
