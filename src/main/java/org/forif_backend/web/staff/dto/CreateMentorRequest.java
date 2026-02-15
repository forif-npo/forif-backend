package org.forif_backend.web.staff.dto;

public record CreateMentorRequest(
    Long userId,
    String password,
    String affiliation
) {
}
