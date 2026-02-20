package org.forif_backend.web.staff.dto;

public record UpdateMentorRequest(
    String name,
    String password,
    String affiliation
) {
}
