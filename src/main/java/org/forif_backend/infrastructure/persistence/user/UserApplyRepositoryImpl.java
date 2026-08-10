package org.forif_backend.infrastructure.persistence.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserApplyRepositoryImpl implements UserApplyRepository {
    private final UserApplyJpaRepository userApplyJpaRepository;

    @Override
    public List<UserApply> findAllUserApplyByUserId(Long userId) {
        return userApplyJpaRepository.findByApplierId(userId);
    }

    @Override
    public List<User> findApplicantsByYearSemester(int year, int semester, String search) {
        return userApplyJpaRepository.findApplicantsByYearSemester(year, semester, search);
    }

    @Override
    public boolean existsByApplierIdAndYearSemester(Long userId, int year, int semester) {
        return userApplyJpaRepository.existsByApplierIdAndApplyYearAndApplySemester(userId, year, semester);
    }

    @Override
    public boolean existsByStudyId(Integer studyId) {
        return userApplyJpaRepository.existsByStudyId(studyId);
    }
}
