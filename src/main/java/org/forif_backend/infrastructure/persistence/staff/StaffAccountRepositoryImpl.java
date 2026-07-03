package org.forif_backend.infrastructure.persistence.staff;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
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
        // 역할이 둘(MENTOR, ADMIN)이면 ADMIN 계정을 대표로 반환
        return staffAccountJpaRepository.findAllByUserIdWithUser(userId).stream()
                .max(Comparator.comparing(sa -> sa.getRole() == StaffRole.ADMIN ? 1 : 0));
    }

    @Override
    public Optional<StaffAccount> findByUserIdAndRole(Long userId, StaffRole role) {
        return staffAccountJpaRepository.findByUserIdAndRole(userId, role);
    }

    @Override
    public List<StaffAccount> findAllByUserId(Long userId) {
        return staffAccountJpaRepository.findAllByUserIdWithUser(userId);
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

    @Override
    public List<StaffAccount> searchWithCursor(Long cursor, int size, String search) {
        return staffAccountQueryRepository.searchWithCursor(cursor, size, search);
    }

    @Override
    public List<StaffAccount> searchMentorsWithOffset(int page, int size, String search) {
        return staffAccountQueryRepository.searchMentorsWithOffset(page, size, search);
    }

    @Override
    public long count(String search) {
        return staffAccountQueryRepository.count(search);
    }

    @Override
    public List<StaffAccount> searchMentorsByYearSemester(int year, int semester, Long cursor, int size, String search) {
        return staffAccountQueryRepository.searchMentorsByYearSemester(year, semester, cursor, size, search);
    }

    @Override
    public List<StaffAccount> searchMentorsByYearSemesterWithOffset(int year, int semester, int page, int size, String search) {
        return staffAccountQueryRepository.searchMentorsByYearSemesterWithOffset(year, semester, page, size, search);
    }

    @Override
    public long countMentorsByYearSemester(int year, int semester, String search) {
        return staffAccountQueryRepository.countMentorsByYearSemester(year, semester, search);
    }
}
