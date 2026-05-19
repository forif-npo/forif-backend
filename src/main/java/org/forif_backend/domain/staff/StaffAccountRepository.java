package org.forif_backend.domain.staff;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StaffAccountRepository {

    Optional<StaffAccount> findByUserId(Long userId); // User ID로 StaffAccount 조회 (User가 @Id이므로 findById와 동일)

    /**
     * 여러 사용자의 StaffRole을 배치 조회
     */
    Map<Long, StaffRole> findStaffRolesByUserIds(List<Long> userIds);

    StaffAccount save(StaffAccount staffAccount);

    Optional<StaffAccount> findById(Long id);

    boolean existsById(Long userId);

    void deleteById(Long userId);

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
