package org.forif_backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.dto.response.ApiErrorData;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ForifException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final List<ApiErrorData> errorDataList;
} 