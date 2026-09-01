package org.forif_backend.application.staff.dto;

public record CreateAdminCommand(
        Long userId,
        String password,
        String affiliation
) {}
