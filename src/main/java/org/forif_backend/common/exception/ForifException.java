package org.forif_backend.common.exception;

import lombok.Getter;
import org.forif_backend.common.dto.response.ApiErrorData;
import java.util.List;

@Getter
public class ForifException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ApiErrorData> errorDataList;

    public ForifException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorDataList = null;
    }

    public ForifException(ErrorCode errorCode, List<ApiErrorData> errorDataList) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorDataList = errorDataList;
    }
}