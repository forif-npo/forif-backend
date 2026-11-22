package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

/**
 * 면접 질문. 부서가 없으면 공통 질문, 있으면 그 부서에 지원한 사람에게만 묻는 질문이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_interview_question")
public class StaffInterviewQuestion extends BaseTimeEntity {

    public static final int MAX_CONTENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_question_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private StaffRecruitment recruitment;

    /** null이면 공통 질문이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_department_id")
    private StaffRecruitmentDepartment department;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private StaffInterviewQuestion(StaffRecruitment recruitment, StaffRecruitmentDepartment department,
                                   String content, int displayOrder) {
        this.recruitment = recruitment;
        this.department = department;
        this.content = content;
        this.displayOrder = displayOrder;
    }

    public static StaffInterviewQuestion create(StaffRecruitment recruitment, StaffRecruitmentDepartment department,
                                                String content, int displayOrder) {
        return new StaffInterviewQuestion(recruitment, department, normalize(content), displayOrder);
    }

    public void updateContent(String content) {
        this.content = normalize(content);
    }

    public boolean isCommon() {
        return this.department == null;
    }

    private static String normalize(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        return trimmed;
    }
}
