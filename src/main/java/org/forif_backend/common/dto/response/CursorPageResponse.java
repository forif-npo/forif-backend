package org.forif_backend.common.dto.response;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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

    /**
     * page가 주어지면 오프셋, 아니면 커서 방식으로 한 페이지를 만든다.
     * 커서 방식의 조회는 다음 페이지 존재 여부를 판별할 수 있게 size + 1건을 읽어와야 한다.
     *
     * @param cursorFetch     size + 1건을 읽는 조회
     * @param cursorExtractor 마지막 항목에서 다음 커서를 뽑는 함수
     */
    public static <E> CursorPageResponse<E> paginate(Integer page, int size, long totalElements,
                                                     Supplier<List<E>> offsetFetch,
                                                     Supplier<List<E>> cursorFetch,
                                                     Function<E, Integer> cursorExtractor) {
        if (page != null) {
            boolean hasNext = (long) (page + 1) * size < totalElements;
            return ofOffset(offsetFetch.get(), hasNext, totalElements, page, size);
        }

        List<E> rows = cursorFetch.get();
        boolean hasNext = rows.size() > size;
        List<E> content = hasNext ? rows.subList(0, size) : rows;
        Integer nextCursor = hasNext ? cursorExtractor.apply(content.get(content.size() - 1)) : null;
        return ofCursor(content, nextCursor, hasNext, totalElements);
    }
}
