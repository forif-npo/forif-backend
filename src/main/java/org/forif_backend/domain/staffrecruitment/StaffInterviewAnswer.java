package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

/** 공통·부서 면접 질문에 대한 면접자의 답변. 회장단이 면접 중에 적는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_interview_answer")
public class StaffInterviewAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_answer_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private StaffApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_question_id", nullable = false)
    private StaffInterviewQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    private StaffInterviewAnswer(StaffApplication application, StaffInterviewQuestion question, String answer) {
        this.application = application;
        this.question = question;
        this.answer = answer;
    }

    public static StaffInterviewAnswer create(StaffApplication application, StaffInterviewQuestion question,
                                              String answer) {
        return new StaffInterviewAnswer(application, question, answer);
    }

    public void updateAnswer(String answer) {
        this.answer = answer;
    }
}
