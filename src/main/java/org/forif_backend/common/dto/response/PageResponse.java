package org.forif_backend.common.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PageResponse<T>(
        int totalPages,
        Long totalElements,
        List<T> content
) {
}
