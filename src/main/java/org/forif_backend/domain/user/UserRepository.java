package org.forif_backend.domain.user;

import org.forif_backend.common.type.SortDirection;
import org.forif_backend.common.type.SortCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    boolean existsById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(Long id);

    Optional<User> findUserById(Long id);

    void createUserApply(UserApply userApply);

    void deleteUserApply(UserApply userApply);

    boolean existUserApply(int year, int semester, User applier);

    Optional<UserApply> findUserApplyByYearAndSemesterAndUser(int year, int semester, User user);

    Page<UserApply> findUserApply(Integer studyId, Pageable pageable, UserApplyStatus statusFilter, SortDirection applyDateDirection);

    Optional<UserApply> findUserApplyById(Long applyId);

    Optional<User> findByPhoneNum(String phoneNum);

    List<User> searchUsersWithCursor(Long cursor, int size, String search);
    List<User> searchUsersWithOffset(int page, int size, String search, List<SortCriteria> sorting);

    long countUsers(String search);

    List<User> searchUsersByYearSemester(int year, int semester, Long cursor, int size, String search);
    List<User> searchUsersByYearSemesterWithOffset(int year, int semester, int page, int size, String search, List<SortCriteria> sorting);

    long countUsersByYearSemester(int year, int semester, String search);

    List<User> searchNotificationUsersWithCursor(Long cursor, int size, String search);

    long countNotificationUsers(String search);

    List<User> searchNotificationUsersByYearSemester(int year, int semester, Long cursor, int size, String search);

    long countNotificationUsersByYearSemester(int year, int semester, String search);

    List<User> searchApplicantsByYearSemester(int year, int semester, Long cursor, int size, String search);

    long countApplicantsByYearSemester(int year, int semester, String search);

    List<User> searchRegularStudyAcceptedApplicantsByYearSemester(int year, int semester, Long cursor, int size, String search);

    long countRegularStudyAcceptedApplicantsByYearSemester(int year, int semester, String search);

    List<User> searchAutonomousStudyAcceptedApplicantsByYearSemester(int year, int semester, Long cursor, int size, String search);

    long countAutonomousStudyAcceptedApplicantsByYearSemester(int year, int semester, String search);

    List<User> searchRejectedApplicantsByYearSemester(int year, int semester, Long cursor, int size, String search);

    long countRejectedApplicantsByYearSemester(int year, int semester, String search);

    List<User> searchAcceptedUsersMissingDuesByYearSemester(int year, int semester, Long cursor, int size, String search);

    long countAcceptedUsersMissingDuesByYearSemester(int year, int semester, String search);

    List<User> searchAcceptedUsersMissingGoogleFormByYearSemester(int year, int semester, Long cursor, int size, String search);

    long countAcceptedUsersMissingGoogleFormByYearSemester(int year, int semester, String search);
}
