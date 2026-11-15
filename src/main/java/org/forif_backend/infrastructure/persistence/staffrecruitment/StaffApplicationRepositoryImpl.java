package org.forif_backend.infrastructure.persistence.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffApplicationRepositoryImpl implements StaffApplicationRepository {

    private final StaffApplicationJpaRepository staffApplicationJpaRepository;

    @Override
    public StaffApplication save(StaffApplication application) {
        return staffApplicationJpaRepository.save(application);
    }

    @Override
    public void flush() {
        staffApplicationJpaRepository.flush();
    }

    @Override
    public Optional<StaffApplication> findDetailById(Integer applicationId) {
        return staffApplicationJpaRepository.findDetailById(applicationId);
    }

    @Override
    public Optional<StaffApplication> findDetailByRecruitmentIdAndApplicantId(Integer recruitmentId,
                                                                              Long applicantUserId) {
        return staffApplicationJpaRepository.findDetailByRecruitmentIdAndApplicantId(recruitmentId, applicantUserId);
    }

    @Override
    public boolean existsByRecruitmentIdAndApplicantId(Integer recruitmentId, Long applicantUserId) {
        return staffApplicationJpaRepository.existsByRecruitmentIdAndApplicantId(recruitmentId, applicantUserId);
    }

    @Override
    public long countForAdmin(Integer recruitmentId, Integer departmentId, String search) {
        return staffApplicationJpaRepository.countForAdmin(recruitmentId, departmentId, search);
    }

    @Override
    public List<StaffApplication> findForAdminWithOffset(Integer recruitmentId, Integer departmentId, String search,
                                                         int page, int size) {
        return staffApplicationJpaRepository.findForAdmin(recruitmentId, departmentId, search, null,
                PageRequest.of(page, size));
    }

    @Override
    public List<StaffApplication> findForAdminWithCursor(Integer recruitmentId, Integer departmentId, String search,
                                                         Integer cursor, int size) {
        // 다음 페이지 존재 여부를 판별할 수 있게 한 건 더 읽는다.
        return staffApplicationJpaRepository.findForAdmin(recruitmentId, departmentId, search, cursor,
                PageRequest.of(0, size + 1));
    }
}
