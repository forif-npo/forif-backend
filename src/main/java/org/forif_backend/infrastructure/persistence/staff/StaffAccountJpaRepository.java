package org.forif_backend.infrastructure.persistence.staff;

import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffAccountJpaRepository extends JpaRepository<StaffAccount, Long> {

    @Query("SELECT sa FROM StaffAccount sa JOIN FETCH sa.user WHERE sa.user.id = :userId AND sa.role = :role")
    Optional<StaffAccount> findByUserIdAndRole(@Param("userId") Long userId, @Param("role") StaffRole role);

    @Query("""
            SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END
            FROM StaffAccount sa WHERE sa.user.id = :userId
            """)
    boolean existsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END
            FROM StaffAccount sa WHERE sa.user.id = :userId AND sa.role = :role
            """)
    boolean existsByUserIdAndRole(@Param("userId") Long userId, @Param("role") StaffRole role);

}
