package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

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

    private StaffInterviewRecord(StaffApplication application, StaffInterviewStatus status) {
        this.application = application;
        this.status = status;
    }

    public static StaffInterviewRecord create(StaffApplication application, StaffInterviewStatus status) {
        return new StaffInterviewRecord(application, status);
    }

    public void changeStatus(StaffInterviewStatus status) {
        this.status = status;
    }
}
