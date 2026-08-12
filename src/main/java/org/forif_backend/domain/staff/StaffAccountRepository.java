package org.forif_backend.domain.staff;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface StaffAccountRepository {

    /**
     * User ID로 대표 StaffAccount 조회.
     * 한 유저가 MENTOR/ADMIN 계정을 모두 가진 경우 ADMIN 계정을 우선 반환한다.
     */
    Optional<StaffAccount> findByUserId(Long userId);

    /**
     * User ID + 역할로 StaffAccount 조회
     */
    Optional<StaffAccount> findByUserIdAndRole(Long userId, StaffRole role);

    /**
     * User ID의 모든 StaffAccount 조회 (최대 2개: MENTOR, ADMIN)
     */
    List<StaffAccount> findAllByUserId(Long userId);

    /**
     * 여러 사용자의 StaffRole을 배치 조회 (역할이 둘이면 ADMIN 우선)
     */
    Map<Long, StaffRole> findStaffRolesByUserIds(List<Long> userIds);

    Set<Long> findMentorAccountUserIdsByUserIds(List<Long> userIds);

    StaffAccount save(StaffAccount staffAccount);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndRole(Long userId, StaffRole role);

    void delete(StaffAccount staffAccount);

    // FOR-72: 운영진(ADMIN) 관련
    List<StaffAccount> searchAdminsWithCursor(Integer cursor, int size, String search);
    List<StaffAccount> searchAdminsWithOffset(int page, int size, String search);

    long countAdmins(String search);

    List<StaffAccount> findByAffiliation(String affiliation);

    // FOR-73: 멘토(MENTOR) 관련
    List<StaffAccount> searchWithCursor(Long cursor, int size, String search);
    List<StaffAccount> searchMentorsWithOffset(int page, int size, String search);

    long count(String search);

    // FOR-96: 학기별 멘토 조회
    List<StaffAccount> searchMentorsByYearSemester(int year, int semester, Long cursor, int size, String search);
    List<StaffAccount> searchMentorsByYearSemesterWithOffset(int year, int semester, int page, int size, String search);

    long countMentorsByYearSemester(int year, int semester, String search);
}
