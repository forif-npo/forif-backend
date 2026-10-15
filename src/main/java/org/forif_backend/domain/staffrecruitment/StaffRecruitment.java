package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 운영진 모집 회차.
 *
 * 스터디의 학기 일정(SemesterPhase)과 달리 독립된 기간을 가지며, 일정이 없으면 접수를 막는다.
 * 활동 학기는 전환되어도 과거 회차가 바뀌지 않도록 값으로 보관한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_recruitment")
public class StaffRecruitment extends BaseTimeEntity {

    /** 게시 중 회차가 하나만 존재하도록 유니크 제약에 쓰는 고정값 */
    private static final int PUBLISHED_SLOT = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_id")
    private Integer id;

    @Column(name = "act_year", nullable = false)
    private int actYear;

    @Column(name = "act_semester", nullable = false)
    private int actSemester;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 200)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRecruitmentStatus status;

    /** PUBLISHED일 때만 1, 그 외에는 null. 유니크 제약으로 동시 게시를 DB에서 막는다. */
    @Column(name = "published_slot")
    private Integer publishedSlot;

    /** 마감·취소가 실제로 적용된 시각. 자연 종료는 endsAt과 같다. */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "close_reason", length = 200)
    private String closeReason;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<StaffRecruitmentDepartment> departments = new ArrayList<>();

    @OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<StaffRecruitmentQuestion> questions = new ArrayList<>();

    private StaffRecruitment(int actYear, int actSemester, String title, String summary, String description,
                             LocalDateTime startsAt, LocalDateTime endsAt) {
        this.actYear = actYear;
        this.actSemester = actSemester;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = StaffRecruitmentStatus.DRAFT;
    }

    public static StaffRecruitment create(int actYear, int actSemester, String title, String summary,
                                          String description, LocalDateTime startsAt, LocalDateTime endsAt) {
        validatePeriod(startsAt, endsAt);
        return new StaffRecruitment(actYear, actSemester, title, summary, description, startsAt, endsAt);
    }

    /** 종단 상태가 아닌 회차의 안내 정보를 수정한다. */
    public void updateDetails(String title, String summary, String description,
                              LocalDateTime startsAt, LocalDateTime endsAt) {
        requireNotTerminal();
        validatePeriod(startsAt, endsAt);
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /**
     * 보낸 목록으로 부서를 맞춘다. id가 있으면 그 부서를 수정하고 없으면 추가하며,
     * 목록에서 빠진 부서는 지운다.
     *
     * 전체 삭제 후 재삽입하지 않는 이유는 두 가지다.
     * 같은 이름을 유지한 수정이 uk_staff_recruitment_department_name에 걸리고,
     * 제출된 지원서가 참조하는 부서를 지울 수 없다.
     *
     * @param hasApplications 제출된 지원서가 있으면 삭제를 거절한다. 모집을 끝낸 부서는 active=false로 숨긴다.
     */
    public void mergeDepartments(List<StaffRecruitmentDepartmentSpec> specs, boolean hasApplications) {
        requireNotTerminal();
        requireNoDuplicateName(specs);

        Map<Integer, StaffRecruitmentDepartment> current = byId(this.departments, StaffRecruitmentDepartment::getId);
        Set<Integer> kept = new LinkedHashSet<>();
        List<StaffRecruitmentDepartment> added = new ArrayList<>();

        for (StaffRecruitmentDepartmentSpec spec : specs) {
            if (spec.id() == null) {
                added.add(StaffRecruitmentDepartment.create(
                        spec.name(), spec.description(), spec.displayOrder(), spec.active()));
                continue;
            }
            StaffRecruitmentDepartment target = current.get(spec.id());
            if (target == null) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
            target.update(spec.name(), spec.description(), spec.displayOrder(), spec.active());
            kept.add(spec.id());
        }

        List<StaffRecruitmentDepartment> removed = this.departments.stream()
                .filter(department -> department.getId() != null && !kept.contains(department.getId()))
                .toList();
        requireRemovable(removed, hasApplications);

        // 지울 부서와 이름이 겹치는 추가는 한 flush 안에서 유니크 제약에 걸린다. id를 보내 수정하는 쪽이 맞다.
        Set<String> removedNames = removed.stream().map(StaffRecruitmentDepartment::getName).collect(Collectors.toSet());
        if (added.stream().anyMatch(department -> removedNames.contains(department.getName()))) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        this.departments.removeAll(removed);
        added.forEach(department -> {
            department.assignTo(this);
            this.departments.add(department);
        });
    }

    /** 부서와 같은 규칙으로 문항을 맞춘다. 제출된 답변이 있으면 문항 삭제를 거절한다. */
    public void mergeQuestions(List<StaffRecruitmentQuestionSpec> specs, boolean hasApplications) {
        requireNotTerminal();

        Map<Integer, StaffRecruitmentQuestion> current = byId(this.questions, StaffRecruitmentQuestion::getId);
        Set<Integer> kept = new LinkedHashSet<>();
        List<StaffRecruitmentQuestion> added = new ArrayList<>();

        for (StaffRecruitmentQuestionSpec spec : specs) {
            if (spec.id() == null) {
                added.add(StaffRecruitmentQuestion.create(spec.label(), spec.description(), spec.type(),
                        spec.required(), spec.maxLength(), spec.displayOrder()));
                continue;
            }
            StaffRecruitmentQuestion target = current.get(spec.id());
            if (target == null) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
            target.update(spec.label(), spec.description(), spec.type(),
                    spec.required(), spec.maxLength(), spec.displayOrder());
            kept.add(spec.id());
        }

        List<StaffRecruitmentQuestion> removed = this.questions.stream()
                .filter(question -> question.getId() != null && !kept.contains(question.getId()))
                .toList();
        requireRemovable(removed, hasApplications);

        this.questions.removeAll(removed);
        added.forEach(question -> {
            question.assignTo(this);
            this.questions.add(question);
        });
    }

    private static <T> Map<Integer, T> byId(List<T> items, Function<T, Integer> idOf) {
        Map<Integer, T> result = new LinkedHashMap<>();
        items.stream().filter(item -> idOf.apply(item) != null)
                .forEach(item -> result.put(idOf.apply(item), item));
        return result;
    }

    private static void requireRemovable(List<?> removed, boolean hasApplications) {
        if (!removed.isEmpty() && hasApplications) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_STRUCTURE_LOCKED);
        }
    }

    private static void requireNoDuplicateName(List<StaffRecruitmentDepartmentSpec> specs) {
        Set<String> names = new LinkedHashSet<>();
        specs.forEach(spec -> {
            if (!names.add(spec.name())) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
        });
    }

    /** 초안만 게시할 수 있고, 기간이 모두 설정돼 있어야 한다. */
    public void publish() {
        if (this.status != StaffRecruitmentStatus.DRAFT) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        }
        if (this.startsAt == null || this.endsAt == null) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_PERIOD);
        }
        this.status = StaffRecruitmentStatus.PUBLISHED;
        this.publishedSlot = PUBLISHED_SLOT;
    }

    /**
     * 게시 중인 회차를 마감한다.
     * 종료 시각이 지난 뒤의 마감은 자연 종료로 보고 endsAt을 기록한다.
     */
    public void close(String reason, LocalDateTime now) {
        if (this.status != StaffRecruitmentStatus.PUBLISHED) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        }
        this.status = StaffRecruitmentStatus.CLOSED;
        this.publishedSlot = null;
        this.endedAt = (this.endsAt != null && !now.isBefore(this.endsAt)) ? this.endsAt : now;
        this.closeReason = reason;
    }

    /** 초안과 게시 중 회차를 취소한다. 취소 시각을 그대로 기록한다. */
    public void cancel(String reason, LocalDateTime now) {
        if (this.status.isTerminal()) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        }
        this.status = StaffRecruitmentStatus.CANCELED;
        this.publishedSlot = null;
        this.endedAt = now;
        this.closeReason = reason;
    }

    /**
     * 공개 응답의 접수 상태를 계산한다. 계약의 판정 순서를 그대로 따른다.
     */
    public RecruitmentApplicationStatus applicationStatus(LocalDateTime now) {
        if (this.status == StaffRecruitmentStatus.CANCELED) {
            return RecruitmentApplicationStatus.CANCELED;
        }
        if (this.status == StaffRecruitmentStatus.CLOSED) {
            return RecruitmentApplicationStatus.CLOSED;
        }
        if (this.status == StaffRecruitmentStatus.DRAFT || this.startsAt == null || this.endsAt == null) {
            return RecruitmentApplicationStatus.CLOSED;
        }
        if (!now.isBefore(this.endsAt)) {
            return RecruitmentApplicationStatus.CLOSED;
        }
        if (now.isBefore(this.startsAt)) {
            return RecruitmentApplicationStatus.UPCOMING;
        }
        return RecruitmentApplicationStatus.OPEN;
    }

    /** 접수 가능 여부. 지원서 제출(3단계)과 화면 노출이 같은 판정을 쓰도록 여기서만 계산한다. */
    public boolean isApplicationOpen(LocalDateTime now) {
        return applicationStatus(now).isOpen();
    }

    public boolean isPubliclyVisible() {
        return this.status.isPubliclyVisible() && this.startsAt != null && this.endsAt != null;
    }

    private void requireNotTerminal() {
        if (this.status.isTerminal()) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        }
    }

    private static void validatePeriod(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_PERIOD);
        }
    }
}
