package org.forif_backend.domain.staff;

import java.util.Optional;

public interface StaffAccountRepository {
    
    Optional<StaffAccount> findByUserId(Long userId); // User ID로 StaffAccount 조회 (User가 @Id이므로 findById와 동일)
    
    StaffAccount save(StaffAccount staffAccount);
    
    Optional<StaffAccount> findById(Long id);

    boolean existsById(Long userId);
}
