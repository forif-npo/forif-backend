package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonSubmission;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionResponse(
        Long submissionId,
        Long hackathonId,
        Long teamId,
        String teamName,
        String projectName,
        String summary,
        String description,
        String githubUrl,
        String deployUrl,
        String imageUrl,
        String presentationFile,
        List<String> techStacks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SubmissionResponse of(HackathonSubmission submission, List<String> techStacks) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getHackathon().getId(),
                submission.getTeam().getId(),
                submission.getTeam().getName(),
                submission.getProjectName(),
                submission.getSummary(),
                submission.getDescription(),
                submission.getGithubUrl(),
                submission.getDeployUrl(),
                submission.getImageUrl(),
                submission.getPresentationFile(),
                techStacks,
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }
}
