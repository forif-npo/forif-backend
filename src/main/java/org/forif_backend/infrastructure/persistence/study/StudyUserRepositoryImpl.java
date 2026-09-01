package org.forif_backend.infrastructure.persistence.study;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudyUserRepositoryImpl implements StudyUserRepository {

    private final StudyUserJpaRepository studyUserJpaRepository;

    @Override
    public Optional<StudyUser> findByUserIdAndStudyId(Long userId, Integer studyId) {
        return studyUserJpaRepository.findByUserIdAndStudyId(userId, studyId);
    }

    @Override
    public List<StudyUser> findAllByStudyId(Integer studyId) {
        return studyUserJpaRepository.findAllByStudyId(studyId);
    }

    @Override
    public List<StudyUser> findAllByUserId(Long userId) {
        return studyUserJpaRepository.findAllByUserId(userId);
    }

    @Override
    public List<User> findUsersByYearSemester(int year, int semester, String search) {
        return studyUserJpaRepository.findUsersByYearSemester(year, semester, search);
    }

    @Override
    public void save(StudyUser studyUser) {
        studyUserJpaRepository.save(studyUser);
    }

    @Override
    public void deleteByUserIdAndStudyId(Long userId, Integer studyId) {
        studyUserJpaRepository.deleteByUserIdAndStudyId(userId, studyId);
    }

    @Override
    public int deleteByUserIdAndStudyYearSemester(Long userId, int year, int semester) {
        return studyUserJpaRepository.deleteByUserIdAndStudyYearSemester(userId, year, semester);
    }

    @Override
    public long countBySemester(int year, int semester) {
        return studyUserJpaRepository.countBySemester(year, semester);
    }

    @Override
    public long countIssuedCertificatesBySemester(int year, int semester) {
        return studyUserJpaRepository.countIssuedCertificatesBySemester(year, semester);
    }

    @Override
    public boolean existsByUserIdAndStudyYearSemester(Long userId, int year, int semester) {
        return studyUserJpaRepository.existsByUserIdAndStudyYearSemester(userId, year, semester);
    }
}
