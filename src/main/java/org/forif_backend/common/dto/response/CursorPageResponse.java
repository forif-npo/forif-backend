package org.forif_backend.common.dto.response;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        Integer nextCursor,
        boolean hasNext,
        long totalElements
) {
}
