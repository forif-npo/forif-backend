package org.forif_backend.infrastructure.persistence.staffrecruitment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlan;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffInterviewPlanRepositoryImpl implements StaffInterviewPlanRepository {

    private final StaffInterviewPlanJpaRepository staffInterviewPlanJpaRepository;
    private final EntityManager entityManager;

    @Override
    public StaffInterviewPlan save(StaffInterviewPlan plan) {
        return staffInterviewPlanJpaRepository.save(plan);
    }

    @Override
    public void flush() {
        staffInterviewPlanJpaRepository.flush();
    }

    @Override
    public void incrementVersion(StaffInterviewPlan plan) {
        entityManager.lock(plan, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    }

    @Override
    public Optional<StaffInterviewPlan> findByRecruitmentId(Integer recruitmentId) {
        return staffInterviewPlanJpaRepository.findDetailByRecruitmentId(recruitmentId);
    }

    @Override
    public void delete(StaffInterviewPlan plan) {
        staffInterviewPlanJpaRepository.delete(plan);
    }
}
