package org.forif_backend.application.staff.dto;

import org.forif_backend.domain.staff.StaffRole;

public record StaffSignUpCommand(
    Long userId,
    String name,
    String password,
    StaffRole role,
    String affiliation
) {
}
