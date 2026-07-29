package org.forif_backend.domain.semester;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

/**
 * 동아리의 현재 활동 학기.
 * 시스템 날짜로 계산하지 않고 운영진이 명시적으로 지정한 값을 단일 행으로 보관한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_active_semester")
public class ActiveSemester extends BaseTimeEntity {

    /** 항상 1. 활동 학기는 동아리에 하나뿐이므로 단일 행으로 강제한다. */
    public static final int SINGLETON_ID = 1;

    @Id
    @Column(name = "active_semester_id")
    private Integer id;

    @Column(name = "act_year", nullable = false)
    private int actYear;

    @Column(name = "act_semester", nullable = false)
    private int actSemester;

    /** 마지막으로 학기를 변경한 운영진 (초기 생성 시 null) */
    @Column(name = "updated_by")
    private Long updatedBy;

    public static ActiveSemester create(int actYear, int actSemester, Long updatedBy) {
        ActiveSemester semester = new ActiveSemester();
        semester.id = SINGLETON_ID;
        semester.actYear = actYear;
        semester.actSemester = actSemester;
        semester.updatedBy = updatedBy;
        return semester;
    }

    public void change(int actYear, int actSemester, Long updatedBy) {
        this.actYear = actYear;
        this.actSemester = actSemester;
        this.updatedBy = updatedBy;
    }

    public boolean isSame(int actYear, int actSemester) {
        return this.actYear == actYear && this.actSemester == actSemester;
    }
}
