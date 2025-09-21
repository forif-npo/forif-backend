package org.forif_backend.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    PRESENT("present", "출석"),
    ABSENT("absent", "결석");

    private final String value;

    private final String label;

    public static AttendanceStatus fromValue(String value) {

        return switch (value) {
            case "present" -> PRESENT;
            case "absent" -> ABSENT;
            default -> throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, "해당 출석 상태가 존재하지 않습니다: " + value);
        };

    }
}