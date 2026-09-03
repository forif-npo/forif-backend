package org.forif_backend.infrastructure.persistence.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.User;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserApplyRepositoryImpl implements UserApplyRepository {
    private final UserApplyJpaRepository userApplyJpaRepository;

    @Override
    public List<UserApply> findAllUserApplyByUserId(Long userId) {
        return userApplyJpaRepository.findByApplierId(userId);
    }

    @Override
    public List<UserApply> findAllByYearSemester(int year, int semester) {
        return userApplyJpaRepository.findAllByYearSemester(year, semester);
    }

    @Override
    public int rejectPendingApplicationsByYearSemester(int year, int semester) {
        return userApplyJpaRepository.rejectPendingStatusesByYearSemester(
                year, semester, UserApplyStatus.PENDING, UserApplyStatus.REJECT);
    }

    @Override
    public List<User> findApplicantsByYearSemester(int year, int semester, String search) {
        return userApplyJpaRepository.findApplicantsByYearSemester(year, semester, search);
    }

    @Override
    public List<User> findAcceptedApplicantsByYearSemester(int year, int semester, String search) {
        return userApplyJpaRepository.findAcceptedApplicantsByYearSemester(
                year, semester, search, UserApplyStatus.ACCEPT);
    }

    @Override
    public boolean existsByApplierIdAndYearSemester(Long userId, int year, int semester) {
        return userApplyJpaRepository.existsByApplierIdAndApplyYearAndApplySemester(userId, year, semester);
    }

    @Override
    public boolean existsAcceptedByApplierIdAndYearSemester(Long userId, int year, int semester) {
        return userApplyJpaRepository.existsAcceptedByApplierIdAndYearSemester(
                userId, year, semester, UserApplyStatus.ACCEPT);
    }

    @Override
    public Optional<UserApply> findByApplierIdAndYearSemester(Long userId, int year, int semester) {
        return userApplyJpaRepository.findByApplier_IdAndApplyYearAndApplySemester(userId, year, semester);
    }

    @Override
    public boolean existsByStudyId(Integer studyId) {
        return userApplyJpaRepository.existsByStudyId(studyId);
    }

    @Override
    public Set<Integer> findStudyIdsWithApplications(List<Integer> studyIds) {
        if (studyIds == null || studyIds.isEmpty()) {
            return Set.of();
        }

        Set<Integer> result = new HashSet<>(
                userApplyJpaRepository.findPrimaryStudyIdsWithApplications(studyIds));
        result.addAll(userApplyJpaRepository.findSecondaryStudyIdsWithApplications(studyIds));
        return result;
    }
}
