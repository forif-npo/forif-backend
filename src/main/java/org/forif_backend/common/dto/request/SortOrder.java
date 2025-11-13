package org.forif_backend.common.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortOrder {
    ASC("asc"),
    DESC("desc");

    private final String value;

    @JsonCreator
    public static SortOrder from(String value) {
        for (SortOrder order: values()) {
            if (order.value.equalsIgnoreCase(value)) {
                return order;
            }
        }

        return null;
    }
}
