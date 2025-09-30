package org.forif_backend.domain.staff;

import java.util.Optional;

public interface StaffAccountRepository {
    
    Optional<StaffAccount> findByLoginId(String loginId);
    
    StaffAccount save(StaffAccount staffAccount);
    
    Optional<StaffAccount> findById(Long id);
}
