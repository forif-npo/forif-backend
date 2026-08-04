package org.forif_backend.application.dues.dto;

public record UpdateDuesMemberCommand(
        Long userId,
        Boolean duesPaid,
        Boolean googleFormSubmitted
) {
}
