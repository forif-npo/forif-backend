package org.forif_backend.domain.semester;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

/**
 * 활동 학기 변경 이력.
 * 학기 전환은 전 서비스에 영향을 주므로 누가 언제 바꿨는지 남긴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_semester_change_log")
public class SemesterChangeLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "from_year", nullable = false)
    private int fromYear;

    @Column(name = "from_semester", nullable = false)
    private int fromSemester;

    @Column(name = "to_year", nullable = false)
    private int toYear;

    @Column(name = "to_semester", nullable = false)
    private int toSemester;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    public static SemesterChangeLog of(int fromYear, int fromSemester,
                                       int toYear, int toSemester, Long changedBy) {
        SemesterChangeLog log = new SemesterChangeLog();
        log.fromYear = fromYear;
        log.fromSemester = fromSemester;
        log.toYear = toYear;
        log.toSemester = toSemester;
        log.changedBy = changedBy;
        return log;
    }
}
