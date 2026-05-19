package org.forif_backend.common.dto.response;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        Integer nextCursor,
        boolean hasNext,
        long totalElements,
        Integer currentPage,
        Integer totalPages
) {
    public static <T> CursorPageResponse<T> ofCursor(List<T> content, Integer nextCursor, boolean hasNext, long totalElements) {
        return new CursorPageResponse<>(content, nextCursor, hasNext, totalElements, null, null);
    }

    public static <T> CursorPageResponse<T> ofOffset(List<T> content, boolean hasNext, long totalElements, int currentPage, int size) {
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 0;
        return new CursorPageResponse<>(content, null, hasNext, totalElements, currentPage, totalPages);
    }

    public <R> CursorPageResponse<R> withContent(List<R> newContent) {
        return new CursorPageResponse<>(newContent, nextCursor, hasNext, totalElements, currentPage, totalPages);
    }
}
