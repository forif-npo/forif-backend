package org.forif_backend.web.hackathon.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SubmissionRequest(
        @NotBlank String projectName,
        @NotBlank String summary,
        String description,
        String githubUrl,
        String deployUrl,
        String imageUrl,
        List<String> techStacks
) {
}
