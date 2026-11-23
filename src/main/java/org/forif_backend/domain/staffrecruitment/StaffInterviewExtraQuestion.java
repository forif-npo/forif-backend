package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

/** 면접자 한 명에게만 묻는 기타 질문과 답변. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_interview_extra_question")
public class StaffInterviewExtraQuestion extends BaseTimeEntity {

    private static final int MAX_CONTENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_extra_question_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private StaffApplication application;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private StaffInterviewExtraQuestion(StaffApplication application, String content, int displayOrder) {
        this.application = application;
        this.content = content;
        this.displayOrder = displayOrder;
    }

    public static StaffInterviewExtraQuestion create(StaffApplication application, String content, int displayOrder) {
        return new StaffInterviewExtraQuestion(application, normalize(content), displayOrder);
    }

    public void updateContent(String content) {
        this.content = normalize(content);
    }

    /** 빈 답변은 아직 적지 않은 것으로 본다. */
    public void updateAnswer(String answer) {
        this.answer = answer == null || answer.isBlank() ? null : answer;
    }

    private static String normalize(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        return trimmed;
    }
}
