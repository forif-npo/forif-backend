package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 모집 회차의 면접 일정 후보.
 *
 * 회장단이 후보 날짜와 수집 시간 범위를 정하면, 지원자는 그 범위 안에서 가능한 시간을 고른다.
 * 슬롯은 한 시간 단위이며 저장하지 않고 날짜와 시간 범위로부터 계산한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_interview_plan")
public class StaffInterviewPlan extends BaseTimeEntity {

    /** 후보 날짜가 너무 많으면 지원자가 고르기 어렵다. */
    public static final int MAX_DATES = 14;
    /** 한 칸의 길이로 쓸 수 있는 값(분). 한 시간을 나누어떨어지게 하는 값만 둔다. */
    public static final List<Integer> ALLOWED_SLOT_MINUTES = List.of(10, 15, 20, 30, 60);
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MIN_HOUR = 0;
    private static final int MAX_HOUR = 24;
    private static final int MAX_GUIDE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_plan_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private StaffRecruitment recruitment;

    @Column(name = "start_hour", nullable = false)
    private int startHour;

    /** 마지막 슬롯의 끝 시각. 9~22면 21시 슬롯이 마지막이다. */
    @Column(name = "end_hour", nullable = false)
    private int endHour;

    /** 한 칸의 길이(분). 회차마다 15·30·60처럼 정한다. */
    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes;

    @Column(nullable = false)
    private boolean required;

    @Column(length = MAX_GUIDE_LENGTH)
    private String guide;

    @Version
    private Long version;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, interviewDate ASC")
    private List<StaffInterviewPlanDate> dates = new ArrayList<>();

    private StaffInterviewPlan(StaffRecruitment recruitment, int startHour, int endHour, int slotMinutes,
                               boolean required, String guide) {
        this.recruitment = recruitment;
        this.startHour = startHour;
        this.endHour = endHour;
        this.slotMinutes = slotMinutes;
        this.required = required;
        this.guide = guide;
    }

    public static StaffInterviewPlan create(StaffRecruitment recruitment, int startHour, int endHour,
                                            int slotMinutes, boolean required, String guide,
                                            List<LocalDate> dates) {
        validateHours(startHour, endHour);
        validateSlotMinutes(slotMinutes);
        validateGuide(guide);
        StaffInterviewPlan plan =
                new StaffInterviewPlan(recruitment, startHour, endHour, slotMinutes, required, guide);
        plan.replaceDates(dates);
        return plan;
    }

    public void update(int startHour, int endHour, int slotMinutes, boolean required, String guide,
                       List<LocalDate> dates) {
        validateHours(startHour, endHour);
        validateSlotMinutes(slotMinutes);
        validateGuide(guide);
        this.startHour = startHour;
        this.endHour = endHour;
        this.slotMinutes = slotMinutes;
        this.required = required;
        this.guide = guide;
        replaceDates(dates);
    }

    /**
     * 후보 날짜를 보낸 목록으로 맞춘다.
     *
     * 전체 삭제 후 재삽입하면 그대로 남는 날짜가 같은 flush 안에서
     * uk_staff_interview_plan_date에 걸린다. 값이 같은 행은 그대로 두고 차이만 반영한다.
     */
    public void replaceDates(List<LocalDate> next) {
        Set<LocalDate> wanted = new LinkedHashSet<>(next == null ? List.of() : next);
        if (wanted.isEmpty()) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT);
        }
        if (wanted.size() > MAX_DATES) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT);
        }

        this.dates.removeIf(date -> !wanted.contains(date.getInterviewDate()));

        int order = 0;
        for (LocalDate date : wanted) {
            order++;
            StaffInterviewPlanDate existing = findDate(date);
            if (existing == null) {
                StaffInterviewPlanDate added = StaffInterviewPlanDate.create(date, order);
                added.assignTo(this);
                this.dates.add(added);
            } else {
                existing.updateOrder(order);
            }
        }
    }

    /**
     * 지원자가 고른 칸이 이 회차의 후보 안에 있는지.
     * 날짜가 후보에 있고, 시작 시각이 수집 범위 안이며, 칸 경계에 맞아야 한다.
     */
    public boolean covers(LocalDate date, int startMinute) {
        if (findDate(date) == null) {
            return false;
        }
        int first = this.startHour * MINUTES_PER_HOUR;
        int last = this.endHour * MINUTES_PER_HOUR;
        return startMinute >= first
                && startMinute < last
                && (startMinute - first) % this.slotMinutes == 0;
    }

    public List<LocalDate> candidateDates() {
        return this.dates.stream().map(StaffInterviewPlanDate::getInterviewDate).toList();
    }

    /** 하루에 생기는 칸 수. */
    public int slotsPerDay() {
        return (this.endHour - this.startHour) * MINUTES_PER_HOUR / this.slotMinutes;
    }

    private StaffInterviewPlanDate findDate(LocalDate date) {
        return this.dates.stream()
                .filter(candidate -> candidate.getInterviewDate().equals(date))
                .findFirst()
                .orElse(null);
    }

    private static void validateHours(int startHour, int endHour) {
        if (startHour < MIN_HOUR || endHour > MAX_HOUR || startHour >= endHour) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT);
        }
    }

    /** 수집 범위를 나누어떨어지게 하는 값만 받는다. 남는 자투리 칸이 생기면 집계가 어긋난다. */
    private static void validateSlotMinutes(int slotMinutes) {
        if (!ALLOWED_SLOT_MINUTES.contains(slotMinutes)) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT);
        }
    }

    private static void validateGuide(String guide) {
        if (guide != null && guide.length() > MAX_GUIDE_LENGTH) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT);
        }
    }
}
