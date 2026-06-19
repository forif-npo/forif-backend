package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_submission", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hackathon_id", "hackathon_team_id"})
})
public class HackathonSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private HackathonEvent hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_team_id", nullable = false)
    private HackathonTeam team;

    @Column(length = 200, nullable = false)
    private String projectName;

    @Column(length = 500, nullable = false)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500, nullable = false)
    private String githubUrl;

    @Column(length = 500)
    private String deployUrl;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String presentationFile;

    public static HackathonSubmission create(HackathonEvent hackathon, HackathonTeam team, String projectName,
                                             String summary, String description, String githubUrl,
                                             String deployUrl, String imageUrl, String presentationFile) {
        HackathonSubmission submission = new HackathonSubmission();
        submission.hackathon = hackathon;
        submission.team = team;
        submission.update(projectName, summary, description, githubUrl, deployUrl, imageUrl, presentationFile);
        return submission;
    }

    public void update(String projectName, String summary, String description, String githubUrl,
                       String deployUrl, String imageUrl, String presentationFile) {
        if (projectName != null) this.projectName = projectName;
        if (summary != null) this.summary = summary;
        if (description != null) this.description = description;
        if (githubUrl != null) this.githubUrl = githubUrl;
        if (deployUrl != null) this.deployUrl = deployUrl;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (presentationFile != null) this.presentationFile = presentationFile;
    }
}
