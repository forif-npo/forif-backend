package org.forif_backend.web.staff.dto;

public record UpdateAdminRequest(
        String name,
        String password,
        String affiliation
) {}
