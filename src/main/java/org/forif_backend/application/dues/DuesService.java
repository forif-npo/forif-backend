package org.forif_backend.application.dues;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.dues.dto.*;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.forif_backend.domain.dues.MemberSemesterCheckRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DuesService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SemesterService semesterService;
    private final StudyUserRepository studyUserRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final UserApplyRepository userApplyRepository;
    private final MemberSemesterCheckRepository memberSemesterCheckRepository;

    /** 표의 컬럼 정렬용. 정렬이 없으면 기존 확인 필요 우선 정렬을 유지한다. */
    public DuesPageResult getCurrentSemesterDues(
            int page,
            int size,
            String search,
            List<SortCriteria> sorting
    ) {
        return getCurrentSemesterDues(page, size, search, null, null, sorting);
    }

    public DuesPageResult getCurrentSemesterDues(
            int page,
            int size,
            String search,
            Boolean duesPaid,
            Boolean googleFormSubmitted,
            List<SortCriteria> sorting
    ) {
        return getCurrentSemesterDues(page, size, search, duesPaid, googleFormSubmitted, comparatorFor(sorting));
    }

    private DuesPageResult getCurrentSemesterDues(
            int page,
            int size,
            String search,
            Boolean duesPaid,
            Boolean googleFormSubmitted,
            Comparator<DuesMember> comparator
    ) {
        SemesterInfo semester = semesterService.getActive();
        List<User> users = findDuesTargets(
                semester.actYear(),
                semester.actSemester(),
                search
        );

        List<DuesMember> members = toDuesMembers(users, semester);
        DuesSummary summary = summarize(members);
        members = members.stream()
                .filter(member -> duesPaid == null || member.duesPaid() == duesPaid)
                .filter(member -> googleFormSubmitted == null
                        || member.googleFormSubmitted() == googleFormSubmitted)
                .sorted(comparator)
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int totalElements = members.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = (int) Math.min((long) safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        return new DuesPageResult(
                semester,
                summary,
                members.subList(fromIndex, toIndex),
                totalElements,
                safePage,
                totalPages,
                safeSize
        );
    }

    @Transactional
    public void updateCurrentSemesterDuesBatch(List<UpdateDuesMemberCommand> commands) {
        SemesterInfo semester = semesterService.getActive();
        commands.forEach(command -> updateCurrentSemesterDues(command, semester));
    }

    private void updateCurrentSemesterDues(
            UpdateDuesMemberCommand command,
            SemesterInfo semester
    ) {
        Long userId = command.userId();
        boolean isAccepted = userApplyRepository.existsAcceptedByApplierIdAndYearSemester(
                userId, semester.actYear(), semester.actSemester());
        if (!isAccepted) {
            throw new ForifException(ErrorCode.CURRENT_SEMESTER_MEMBER_NOT_FOUND);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        MemberSemesterCheck memberCheck = memberSemesterCheckRepository
                .findByUserIdAndYearSemester(userId, semester.actYear(), semester.actSemester())
                .orElseGet(() -> MemberSemesterCheck.create(user, semester.actYear(), semester.actSemester()));
        memberCheck.update(command.duesPaid(), command.googleFormSubmitted());
        memberSemesterCheckRepository.save(memberCheck);
        synchronizeStudyMembership(user, semester, memberCheck);
    }

    @Transactional
    public void ensureMemberCheck(Study study, User user) {
        memberSemesterCheckRepository
                .findByUserIdAndYearSemester(user.getId(), study.getActYear(), study.getActSemester())
                .orElseGet(() -> memberSemesterCheckRepository.save(
                        MemberSemesterCheck.create(user, study.getActYear(), study.getActSemester())
                ));
    }

    /**
     * 스터디 합격자 중 회비 납부와 구글폼 제출을 모두 확인한 경우에만 수강생으로 등록한다.
     */
    @Transactional
    public void registerStudyUserIfEligible(Study study, User user) {
        memberSemesterCheckRepository
                .findByUserIdAndYearSemester(user.getId(), study.getActYear(), study.getActSemester())
                .ifPresent(memberCheck -> registerStudyUserIfEligible(study, user, memberCheck));
    }

    private void registerStudyUserIfEligible(Study study, User user, MemberSemesterCheck memberCheck) {
        if (!memberCheck.isDuesPaid() || !memberCheck.isGoogleFormSubmitted()) {
            return;
        }
        studyUserRepository.findByUserIdAndStudyId(user.getId(), study.getId())
                .orElseGet(() -> {
                    StudyUser studyUser = StudyUser.create(study, user);
                    studyUserRepository.save(studyUser);
                    return studyUser;
                });
    }

    private void synchronizeStudyMembership(
            User user,
            SemesterInfo semester,
            MemberSemesterCheck memberCheck
    ) {
        userApplyRepository.findByApplierIdAndYearSemester(
                        user.getId(), semester.actYear(), semester.actSemester())
                .flatMap(this::acceptedStudyId)
                .flatMap(studyRepository::findStudyById)
                .ifPresent(study -> {
                    if (memberCheck.isDuesPaid() && memberCheck.isGoogleFormSubmitted()) {
                        registerStudyUserIfEligible(study, user, memberCheck);
                    } else {
                        studyUserRepository.deleteByUserIdAndStudyId(user.getId(), study.getId());
                    }
                });
    }

    private Optional<Integer> acceptedStudyId(UserApply apply) {
        if (apply.getPrimaryStatus() == UserApplyStatus.ACCEPT) {
            return Optional.of(apply.getPrimaryStudy());
        }
        if (apply.getSecondaryStatus() == UserApplyStatus.ACCEPT) {
            return Optional.ofNullable(apply.getSecondaryStudy());
        }
        return Optional.empty();
    }

    private List<User> findDuesTargets(int year, int semester, String search) {
        return userApplyRepository.findAcceptedApplicantsByYearSemester(year, semester, search);
    }

    private List<DuesMember> toDuesMembers(List<User> users, SemesterInfo semester) {
        if (users.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, MemberSemesterCheck> memberChecks = memberSemesterCheckRepository
                .findAllByYearSemesterAndUserIds(semester.actYear(), semester.actSemester(), userIds)
                .stream()
                .collect(Collectors.toMap(memberCheck -> memberCheck.getUser().getId(), Function.identity()));
        return users.stream()
                .map(user -> toDuesMember(user, memberChecks.get(user.getId())))
                .toList();
    }

    private DuesMember toDuesMember(User user, MemberSemesterCheck memberCheck) {
        return new DuesMember(
                user.getId(),
                user.getUserName(),
                user.getDepartment(),
                memberCheck != null && memberCheck.isDuesPaid(),
                memberCheck != null && memberCheck.isGoogleFormSubmitted()
        );
    }

    private Comparator<DuesMember> defaultComparator() {
        Comparator<DuesMember> byName = Comparator
                .comparing(DuesMember::userName, Comparator.nullsLast(String::compareTo))
                .thenComparing(DuesMember::userId);

        return Comparator.comparingInt(this::attentionPriority)
                .thenComparing(byName);
    }

    private Comparator<DuesMember> comparatorFor(List<SortCriteria> sorting) {
        if (sorting == null || sorting.isEmpty()) {
            return defaultComparator();
        }
        Comparator<DuesMember> result = null;
        for (SortCriteria criteria : sorting) {
            Comparator<DuesMember> next = switch (criteria.field()) {
                case "userId" -> Comparator.comparing(DuesMember::userId);
                case "userName" -> Comparator.comparing(DuesMember::userName,
                        Comparator.nullsLast(String::compareTo));
                case "department" -> Comparator.comparing(DuesMember::department,
                        Comparator.nullsLast(String::compareTo));
                case "googleFormSubmitted" -> Comparator.comparing(DuesMember::googleFormSubmitted);
                case "duesPaid" -> Comparator.comparing(DuesMember::duesPaid);
                default -> throw new ForifException(ErrorCode.INVALID_INPUT);
            };
            if (criteria.direction() == SortDirection.DESC) {
                next = next.reversed();
            }
            result = result == null ? next : result.thenComparing(next);
        }
        return result.thenComparing(DuesMember::userId);
    }

    private int attentionPriority(DuesMember member) {
        if (!member.googleFormSubmitted() && !member.duesPaid()) {
            return 0;
        }
        if (!member.googleFormSubmitted()) {
            return 1;
        }
        if (!member.duesPaid()) {
            return 2;
        }
        return 3;
    }

    private DuesSummary summarize(List<DuesMember> members) {
        int duesPaidCount = (int) members.stream().filter(DuesMember::duesPaid).count();
        int googleFormSubmittedCount = (int) members.stream().filter(DuesMember::googleFormSubmitted).count();
        int completedCount = (int) members.stream()
                .filter(member -> member.duesPaid() && member.googleFormSubmitted())
                .count();
        return new DuesSummary(members.size(), duesPaidCount, googleFormSubmittedCount, completedCount);
    }
}
