package org.forif_backend.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final long timestamp;
    private final T data;
    private final String errorCode;
    private final String message;


}
