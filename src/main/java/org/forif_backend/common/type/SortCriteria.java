package org.forif_backend.common.type;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 목록 API에서 허용된 컬럼만 정렬하도록 제한하는 정렬 조건이다. */
public record SortCriteria(String field, SortDirection direction) {

    public static List<SortCriteria> parse(List<String> values, Set<String> allowedFields) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(value -> parse(value, allowedFields))
                .toList();
    }

    private static SortCriteria parse(String value, Set<String> allowedFields) {
        String[] parts = value == null ? new String[0] : value.split(":", -1);
        if (parts.length != 2 || !allowedFields.contains(parts[0])) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        try {
            return new SortCriteria(parts[0], SortDirection.valueOf(parts[1].toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }
}
