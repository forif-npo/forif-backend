package org.forif_backend.domain.staff;

import java.util.Optional;
import java.util.List;
import java.util.Map;

public interface StaffAccountRepository {

    /** User ID로 운영진 계정을 조회한다. */
    Optional<StaffAccount> findByUserId(Long userId);

    /**
     * User ID + 역할로 StaffAccount 조회
     */
    Optional<StaffAccount> findByUserIdAndRole(Long userId, StaffRole role);

    /** 여러 사용자의 운영진 역할을 배치 조회한다. */
    Map<Long, StaffRole> findStaffRolesByUserIds(List<Long> userIds);

    StaffAccount save(StaffAccount staffAccount);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndRole(Long userId, StaffRole role);

    void delete(StaffAccount staffAccount);

    // FOR-72: 운영진(ADMIN) 관련
    List<StaffAccount> searchAdminsWithCursor(Integer cursor, int size, String search);
    List<StaffAccount> searchAdminsWithOffset(int page, int size, String search);

    long countAdmins(String search);

    List<StaffAccount> findByAffiliation(String affiliation);

}
