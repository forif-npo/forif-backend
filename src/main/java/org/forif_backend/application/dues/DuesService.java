package org.forif_backend.application.dues;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.dues.dto.*;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.forif_backend.domain.dues.MemberSemesterCheckRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final MemberSemesterCheckRepository memberSemesterCheckRepository;

    public DuesPageResult getCurrentSemesterDues(
            int page,
            int size,
            String search,
            DuesSort sort
    ) {
        SemesterInfo semester = semesterService.getActive();
        List<User> users = studyUserRepository.findUsersByYearSemester(
                semester.actYear(),
                semester.actSemester(),
                search
        );

        List<DuesMember> members = toDuesMembers(users, semester);
        members = members.stream()
                .sorted(comparatorFor(sort))
                .toList();

        DuesSummary summary = summarize(members);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int totalElements = members.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
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
    public List<DuesMember> updateCurrentSemesterDuesBatch(List<UpdateDuesMemberCommand> commands) {
        SemesterInfo semester = semesterService.getActive();
        return commands.stream()
                .map(command -> updateCurrentSemesterDues(command, semester))
                .toList();
    }

    private DuesMember updateCurrentSemesterDues(
            UpdateDuesMemberCommand command,
            SemesterInfo semester
    ) {
        Long userId = command.userId();
        if (!studyUserRepository.existsByUserIdAndStudyYearSemester(
                userId,
                semester.actYear(),
                semester.actSemester()
        )) {
            throw new ForifException(ErrorCode.USER_NOT_FOUND);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        MemberSemesterCheck memberCheck = memberSemesterCheckRepository
                .findByUserIdAndYearSemester(userId, semester.actYear(), semester.actSemester())
                .orElseGet(() -> MemberSemesterCheck.create(user, semester.actYear(), semester.actSemester()));
        memberCheck.update(command.duesPaid(), command.googleFormSubmitted());
        memberSemesterCheckRepository.save(memberCheck);

        return toDuesMember(user, semester, memberCheck);
    }

    @Transactional
    public void ensureMemberCheck(Study study, User user) {
        memberSemesterCheckRepository
                .findByUserIdAndYearSemester(user.getId(), study.getActYear(), study.getActSemester())
                .orElseGet(() -> memberSemesterCheckRepository.save(
                        MemberSemesterCheck.create(user, study.getActYear(), study.getActSemester())
                ));
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
        Map<Long, String> studyNames = studyRepository.findCurrentStudyNamesByUserIds(
                userIds,
                semester.actYear(),
                semester.actSemester()
        );

        return users.stream()
                .map(user -> toDuesMember(user, studyNames.get(user.getId()), memberChecks.get(user.getId())))
                .toList();
    }

    private DuesMember toDuesMember(User user, SemesterInfo semester, MemberSemesterCheck memberCheck) {
        String studyName = studyRepository.findCurrentStudyNamesByUserIds(
                List.of(user.getId()),
                semester.actYear(),
                semester.actSemester()
        ).get(user.getId());
        return toDuesMember(user, studyName, memberCheck);
    }

    private DuesMember toDuesMember(User user, String studyName, MemberSemesterCheck memberCheck) {
        return new DuesMember(
                user.getId(),
                user.getUserName(),
                user.getDepartment(),
                studyName,
                memberCheck != null && memberCheck.isDuesPaid(),
                memberCheck != null && memberCheck.isGoogleFormSubmitted()
        );
    }

    private Comparator<DuesMember> comparatorFor(DuesSort sort) {
        DuesSort effectiveSort = sort == null ? DuesSort.NEEDS_ATTENTION : sort;
        Comparator<DuesMember> byName = Comparator
                .comparing(DuesMember::userName, Comparator.nullsLast(String::compareTo))
                .thenComparing(DuesMember::userId);

        return switch (effectiveSort) {
            case GOOGLE_FORM_SUBMITTED -> Comparator
                    .comparing(DuesMember::googleFormSubmitted)
                    .thenComparing(byName);
            case DUES_PAID -> Comparator
                    .comparing(DuesMember::duesPaid)
                    .thenComparing(byName);
            case NAME -> byName;
            case NEEDS_ATTENTION -> Comparator
                    .comparingInt(this::attentionPriority)
                    .thenComparing(byName);
        };
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
