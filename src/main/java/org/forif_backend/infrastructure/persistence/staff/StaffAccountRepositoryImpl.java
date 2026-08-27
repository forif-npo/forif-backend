package org.forif_backend.infrastructure.persistence.staff;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffAccountRepositoryImpl implements StaffAccountRepository {

    private final StaffAccountJpaRepository staffAccountJpaRepository;
    private final StaffAccountQueryRepository staffAccountQueryRepository;

    @Override
    public Optional<StaffAccount> findByUserId(Long userId) {
        return staffAccountJpaRepository.findByUserIdAndRole(userId, StaffRole.ADMIN);
    }

    @Override
    public Optional<StaffAccount> findByUserIdAndRole(Long userId, StaffRole role) {
        return staffAccountJpaRepository.findByUserIdAndRole(userId, role);
    }

    @Override
    public Map<Long, StaffRole> findStaffRolesByUserIds(List<Long> userIds) {
        return staffAccountQueryRepository.findStaffRolesByUserIds(userIds);
    }

    @Override
    public StaffAccount save(StaffAccount staffAccount) {
        return staffAccountJpaRepository.save(staffAccount);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return staffAccountJpaRepository.existsByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndRole(Long userId, StaffRole role) {
        return staffAccountJpaRepository.existsByUserIdAndRole(userId, role);
    }

    @Override
    public void delete(StaffAccount staffAccount) {
        staffAccountJpaRepository.delete(staffAccount);
    }

    @Override
    public List<StaffAccount> searchAdminsWithCursor(Integer cursor, int size, String search) {
        return staffAccountQueryRepository.searchAdminsWithCursor(cursor, size, search);
    }

    @Override
    public List<StaffAccount> searchAdminsWithOffset(int page, int size, String search) {
        return staffAccountQueryRepository.searchAdminsWithOffset(page, size, search);
    }

    @Override
    public long countAdmins(String search) {
        return staffAccountQueryRepository.countAdmins(search);
    }

    @Override
    public List<StaffAccount> findByAffiliation(String affiliation) {
        return staffAccountQueryRepository.findByAffiliation(affiliation);
    }

}
