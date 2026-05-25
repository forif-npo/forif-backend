package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonSubmission;

import java.time.LocalDateTime;
import java.util.List;

public record ArchiveSubmissionDetailResponse(
        Long submissionId,
        Long hackathonId,
        Long teamId,
        String teamName,
        String projectName,
        String summary,
        String description,
        String githubUrl,
        String deployUrl,
        String presentationFile,
        List<String> techStacks,
        List<TeamMemberResponse> teamMembers,
        boolean awarded,
        List<AwardResponse> awards,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ArchiveSubmissionDetailResponse of(HackathonSubmission submission,
                                                     List<String> techStacks,
                                                     List<TeamMemberResponse> teamMembers,
                                                     List<AwardResponse> awards) {
        return new ArchiveSubmissionDetailResponse(
                submission.getId(),
                submission.getHackathon().getId(),
                submission.getTeam().getId(),
                submission.getTeam().getName(),
                submission.getProjectName(),
                submission.getSummary(),
                submission.getDescription(),
                submission.getGithubUrl(),
                submission.getDeployUrl(),
                submission.getPresentationFile(),
                techStacks,
                teamMembers,
                !awards.isEmpty(),
                awards,
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }
}
