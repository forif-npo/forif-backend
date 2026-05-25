package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_submission_tech_stack", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"submission_id", "display_order"})
})
public class HackathonSubmissionTechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_stack_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private HackathonSubmission submission;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    private int displayOrder;

    public static HackathonSubmissionTechStack create(HackathonSubmission submission, String name, int displayOrder) {
        HackathonSubmissionTechStack techStack = new HackathonSubmissionTechStack();
        techStack.submission = submission;
        techStack.name = name;
        techStack.displayOrder = displayOrder;
        return techStack;
    }
}
