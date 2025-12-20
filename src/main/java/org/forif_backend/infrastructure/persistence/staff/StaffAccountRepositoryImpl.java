package org.forif_backend.infrastructure.persistence.staff;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffAccountRepositoryImpl implements StaffAccountRepository {

    private final StaffAccountJpaRepository staffAccountJpaRepository;

    @Override
    public Optional<StaffAccount> findByUserId(Long userId) {
        return staffAccountJpaRepository.findById(userId);
    }

    // 미사용
    @Override
    public StaffAccount save(StaffAccount staffAccount) {
        return staffAccountJpaRepository.save(staffAccount);
    }

    // 미사용
    @Override
    public Optional<StaffAccount> findById(Long id) {
        return staffAccountJpaRepository.findById(id);
    }

    @Override
    public boolean existsById(Long userId) {
        return staffAccountJpaRepository.existsById(userId);
    }
}
