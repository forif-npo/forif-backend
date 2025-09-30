package org.forif_backend.domain.staff;

import java.util.Optional;

public interface StaffAccountRepository {
    
    Optional<StaffAccount> findByUserId(Long userId);
    
    StaffAccount save(StaffAccount staffAccount);
    
    Optional<StaffAccount> findById(Long id);
}
