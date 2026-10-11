package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자기소개서 답변.
 * 문항이 나중에 바뀌어도 제출 당시 의미가 남도록 label과 type을 함께 보관한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_application_answer")
public class StaffApplicationAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_answer_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private StaffApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_question_id", nullable = false)
    private StaffRecruitmentQuestion question;

    @Column(name = "question_label", nullable = false, length = 100)
    private String questionLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private StaffRecruitmentQuestionType questionType;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private StaffApplicationAnswer(StaffRecruitmentQuestion question, String answer) {
        this.question = question;
        this.questionLabel = question.getLabel();
        this.questionType = question.getType();
        this.answer = answer;
    }

    public static StaffApplicationAnswer create(StaffRecruitmentQuestion question, String answer) {
        return new StaffApplicationAnswer(question, answer);
    }

    void assignTo(StaffApplication application) {
        this.application = application;
    }
}
