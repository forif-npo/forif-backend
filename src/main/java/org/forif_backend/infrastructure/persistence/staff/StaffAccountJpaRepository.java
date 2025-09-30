package org.forif_backend.infrastructure.persistence.staff;

import org.forif_backend.domain.staff.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffAccountJpaRepository extends JpaRepository<StaffAccount, Long> {
    
    Optional<StaffAccount> findByUser_Id(Long userId);
}
