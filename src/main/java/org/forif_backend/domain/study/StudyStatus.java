package org.forif_backend.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@Getter
@RequiredArgsConstructor
public enum StudyStatus {
    PENDING("PENDING", "대기"),
    APPROVED("APPROVED", "승인"),
    REJECTED("REJECTED", "거절"),
    RE_APPLIED("RE_APPLIED", "재요청");

    private final String value;
    private final String description;

    public static StudyStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (StudyStatus status : StudyStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, "Invalid status: " + value);
    }
}