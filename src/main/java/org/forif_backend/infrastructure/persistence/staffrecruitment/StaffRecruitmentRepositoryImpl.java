package org.forif_backend.infrastructure.persistence.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffRecruitmentRepositoryImpl implements StaffRecruitmentRepository {

    private final StaffRecruitmentJpaRepository staffRecruitmentJpaRepository;

    @Override
    public StaffRecruitment save(StaffRecruitment recruitment) {
        return staffRecruitmentJpaRepository.save(recruitment);
    }

    @Override
    public void flush() {
        staffRecruitmentJpaRepository.flush();
    }

    @Override
    public Optional<StaffRecruitment> findById(Integer recruitmentId) {
        return staffRecruitmentJpaRepository.findById(recruitmentId);
    }

    @Override
    public Optional<StaffRecruitment> findDetailById(Integer recruitmentId) {
        return staffRecruitmentJpaRepository.findDetailById(recruitmentId);
    }

    @Override
    public Optional<StaffRecruitment> findByIdForUpdate(Integer recruitmentId) {
        return staffRecruitmentJpaRepository.findByIdForUpdate(recruitmentId);
    }

    @Override
    public Optional<StaffRecruitment> findPublished() {
        return staffRecruitmentJpaRepository.findByStatus(StaffRecruitmentStatus.PUBLISHED);
    }

    @Override
    public List<StaffRecruitment> findAllForAdmin(StaffRecruitmentStatus status, int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / Math.max(limit, 1), Math.max(limit, 1));
        return status == null
                ? staffRecruitmentJpaRepository.findAllByOrderByIdDesc(pageable)
                : staffRecruitmentJpaRepository.findAllByStatusOrderByIdDesc(status, pageable);
    }

    @Override
    public long countForAdmin(StaffRecruitmentStatus status) {
        return status == null
                ? staffRecruitmentJpaRepository.count()
                : staffRecruitmentJpaRepository.countByStatus(status);
    }
}
