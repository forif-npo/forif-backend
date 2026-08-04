package org.forif_backend.application.dues.dto;

public record UpdateDuesCommand(
        Boolean duesPaid,
        Boolean googleFormSubmitted
) {
}
