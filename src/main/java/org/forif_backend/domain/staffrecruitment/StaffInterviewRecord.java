package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

/** 면접자의 진행 상태. 기록이 없는 지원자는 면접 전이다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_interview_record")
public class StaffInterviewRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_record_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private StaffApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffInterviewStatus status;

    /** 합격자에게 배정한 부서. 지원자의 1지망·2지망 중 하나이고, 합격이 아니면 비어 있다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passed_department_id")
    private StaffRecruitmentDepartment passedDepartment;

    private StaffInterviewRecord(StaffApplication application) {
        this.application = application;
        this.status = StaffInterviewStatus.BEFORE;
    }

    public static StaffInterviewRecord create(StaffApplication application, StaffInterviewStatus status,
                                              StaffRecruitmentDepartment passedDepartment) {
        StaffInterviewRecord record = new StaffInterviewRecord(application);
        record.changeStatus(status, passedDepartment);
        return record;
    }

    /** 합격이면 배정 부서가 꼭 있어야 하고, 합격이 아니면 배정 부서를 비운다. */
    public void changeStatus(StaffInterviewStatus status, StaffRecruitmentDepartment passedDepartment) {
        if (status == StaffInterviewStatus.PASSED && passedDepartment == null) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        this.status = status;
        this.passedDepartment = status == StaffInterviewStatus.PASSED ? passedDepartment : null;
    }
}
