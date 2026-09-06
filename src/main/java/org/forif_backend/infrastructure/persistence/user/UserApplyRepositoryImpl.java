package org.forif_backend.infrastructure.persistence.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.User;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public Map<Long, String> findAcceptedStudyNamesByUserIdsAndYearSemester(
            List<Long> userIds, int year, int semester
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> studyNames = new LinkedHashMap<>();
        userApplyJpaRepository.findAllByYearSemesterAndApplierIds(year, semester, userIds)
                .forEach(application -> {
                    String acceptedStudyName = acceptedStudyName(application);
                    if (acceptedStudyName != null) {
                        studyNames.put(application.getApplier().getId(), acceptedStudyName);
                    }
                });
        return studyNames;
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

    private String acceptedStudyName(UserApply application) {
        if (application.getPrimaryStatus() == UserApplyStatus.ACCEPT) {
            return application.getPrimaryStudyName();
        }
        if (application.getSecondaryStatus() == UserApplyStatus.ACCEPT) {
            return application.getSecondaryStudyName();
        }
        return null;
    }
}
