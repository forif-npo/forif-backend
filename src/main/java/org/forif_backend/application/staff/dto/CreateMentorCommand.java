package org.forif_backend.application.staff.dto;

public record CreateMentorCommand(
    Long userId,
    String password,
    String affiliation
) {
}
