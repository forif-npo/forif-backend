package org.forif_backend.infrastructure.persistence.staff;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffAccountRepositoryImpl implements StaffAccountRepository {

    private final StaffAccountJpaRepository staffAccountJpaRepository;
    private final StaffAccountQueryRepository staffAccountQueryRepository;

    @Override
    public Optional<StaffAccount> findByUserId(Long userId) {
        return staffAccountJpaRepository.findByIdWithUser(userId);
    }

    @Override
    public StaffAccount save(StaffAccount staffAccount) {
        return staffAccountJpaRepository.save(staffAccount);
    }

    @Override
    public Optional<StaffAccount> findById(Long id) {
        return staffAccountJpaRepository.findById(id);
    }

    @Override
    public boolean existsById(Long userId) {
        return staffAccountJpaRepository.existsById(userId);
    }

    @Override
    public void deleteById(Long userId) {
        staffAccountJpaRepository.deleteById(userId);
    }

    @Override
    public List<StaffAccount> searchAdminsWithCursor(Integer cursor, int size, String search) {
        return staffAccountQueryRepository.searchAdminsWithCursor(cursor, size, search);
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
    public long count(String search) {
        return staffAccountQueryRepository.count(search);
    }
}
