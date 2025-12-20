package org.forif_backend.infrastructure.persistence.staff;

import org.forif_backend.domain.staff.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffAccountJpaRepository extends JpaRepository<StaffAccount, Long> {

    @Query("SELECT sa FROM StaffAccount sa JOIN FETCH sa.user WHERE sa.id = :userId")
    Optional<StaffAccount> findByIdWithUser(@Param("userId") Long userId);
}
