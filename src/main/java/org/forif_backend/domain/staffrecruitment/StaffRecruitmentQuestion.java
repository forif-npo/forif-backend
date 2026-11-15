package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모집 회차의 자기소개서 문항.
 * 제출된 답변은 3단계에서 문항 label·type을 함께 보관하므로 문항이 바뀌어도 과거 답변의 의미가 유지된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_recruitment_question")
public class StaffRecruitmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_question_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private StaffRecruitment recruitment;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRecruitmentQuestionType type;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "max_length", nullable = false)
    private int maxLength;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private StaffRecruitmentQuestion(String label, String description, StaffRecruitmentQuestionType type,
                                     boolean required, int maxLength, int displayOrder) {
        this.label = label;
        this.description = description;
        this.type = type;
        this.required = required;
        this.maxLength = maxLength;
        this.displayOrder = displayOrder;
    }

    public static StaffRecruitmentQuestion create(String label, String description, StaffRecruitmentQuestionType type,
                                                  boolean required, int maxLength, int displayOrder) {
        return new StaffRecruitmentQuestion(label, description, type, required, maxLength, displayOrder);
    }

    void assignTo(StaffRecruitment recruitment) {
        this.recruitment = recruitment;
    }
}
