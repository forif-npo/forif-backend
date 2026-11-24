package org.forif_backend.infrastructure.persistence.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationInterviewSlot;
import org.forif_backend.domain.staffrecruitment.StaffApplicationRepository;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
    public void delete(StaffApplication application) {
        staffApplicationJpaRepository.delete(application);
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
    public Optional<StaffApplication> findForMutation(Integer recruitmentId, Long applicantUserId) {
        return staffApplicationJpaRepository.findForMutation(recruitmentId, applicantUserId);
    }

    @Override
    public List<StaffApplication> findByApplicantId(Long applicantUserId) {
        return staffApplicationJpaRepository.findByApplicantId(applicantUserId);
    }

    @Override
    public boolean existsByRecruitmentIdAndApplicantId(Integer recruitmentId, Long applicantUserId) {
        return staffApplicationJpaRepository.existsByRecruitmentIdAndApplicantId(recruitmentId, applicantUserId);
    }

    @Override
    public boolean existsByRecruitmentId(Integer recruitmentId) {
        return staffApplicationJpaRepository.existsByRecruitmentId(recruitmentId);
    }

    @Override
    public List<StaffApplication> findAllWithChoicesByRecruitmentId(Integer recruitmentId) {
        return staffApplicationJpaRepository.findAllWithChoicesByRecruitmentId(recruitmentId);
    }

    @Override
    public List<StaffApplicationInterviewSlot> findInterviewSlotsByRecruitmentId(Integer recruitmentId) {
        return staffApplicationJpaRepository.findInterviewSlotsByRecruitmentId(recruitmentId);
    }

    @Override
    public long countForAdmin(Integer recruitmentId, Integer departmentId, String search) {
        return staffApplicationJpaRepository.countForAdmin(recruitmentId, departmentId, search);
    }

    @Override
    public List<StaffApplication> findForAdminWithOffset(Integer recruitmentId, Integer departmentId, String search,
                                                         int page, int size, List<SortCriteria> sorting) {
        return staffApplicationJpaRepository.findForAdminPage(recruitmentId, departmentId, search,
                PageRequest.of(page, size, adminSort(sorting)));
    }

    /** 허용 필드는 컨트롤러가 거른다. 같은 값끼리는 ID 내림차순이 되도록 id를 마지막에 둔다. */
    private Sort adminSort(List<SortCriteria> sorting) {
        List<Sort.Order> orders = new ArrayList<>();
        for (SortCriteria criterion : sorting) {
            String property = switch (criterion.field()) {
                case "department" -> "applicantDepartment";
                case "submittedAt" -> "submittedAt";
                default -> throw new IllegalStateException("Unsupported staff application sort field: "
                        + criterion.field());
            };
            orders.add(criterion.direction() == SortDirection.ASC
                    ? Sort.Order.asc(property) : Sort.Order.desc(property));
        }
        orders.add(Sort.Order.desc("id"));
        return Sort.by(orders);
    }

    @Override
    public List<StaffApplication> findForAdminWithCursor(Integer recruitmentId, Integer departmentId, String search,
                                                         Integer cursor, int size) {
        // 다음 페이지 존재 여부를 판별할 수 있게 한 건 더 읽는다.
        return staffApplicationJpaRepository.findForAdmin(recruitmentId, departmentId, search, cursor,
                PageRequest.of(0, size + 1));
    }
}
